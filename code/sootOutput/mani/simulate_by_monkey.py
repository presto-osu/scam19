#!/usr/bin/env python3

import argparse
import glob
import os
import shutil
import subprocess
import sys
import time
import json

import gorilla
import xmltodict
from gorilla import info, warn, err, Gorilla, get_devices, progress, kill_proc

MANI_DIR = os.path.realpath(os.path.dirname(__file__))
GATOR_DIR = os.path.realpath(os.path.join(MANI_DIR, '..', '..'))
GATOR = os.path.join(GATOR_DIR, 'gator')

ADB = os.path.join(os.environ['ANDROID_SDK'], 'platform-tools', 'adb')
EMULATOR = os.path.join(os.environ['ANDROID_SDK'], 'emulator', 'emulator')


def newline():
    sys.stdout.write('\n')
    sys.stdout.flush()


def instrument(apk, apk_name, ga_id):
    if not os.path.isfile('%s/%s' % (MANI_DIR, apk_name)):
        warn('Instrument and sign...')
        cmd = '%s i -x xml/%s.xml -p %s --experiment --immediate' % (
            GATOR, apk_name, os.path.realpath(apk))
        if ga_id:
            cmd = '%s --id %s' % (cmd, ga_id)
        info(cmd)
        proc = subprocess.Popen(cmd.split(),
                                cwd=GATOR_DIR,
                                stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT,
                                universal_newlines=True)
        has_send = False
        while True:
            inline = proc.stdout.readline()
            progress('^')
            if not inline or proc.poll():
                newline()
                break
            if 'Replace Tracker.send()' in inline:
                has_send = True
        if not has_send:
            err('%s do not have send' % apk_name)
            return False

        cmd = 'bash sign.sh %s' % apk_name
        info(cmd)
        subprocess.call(cmd.split(), stdout=subprocess.DEVNULL, cwd=MANI_DIR)


# settings put global airplane_mode_on 0
# am broadcast -a android.intent.action.AIRPLANE_MODE
# svc wifi enable
def run(apk, avd, dev, ga_id, num_nodes, fix_degree_per_node, start_idx,
        num_runs, window, throttle, num_events):
    apk_name = apk.split('/')[-1]
    info('---------------------- %s on %s -----------------' % (apk_name, dev))

    pkg_name = apk_name[:-len('.apk')]
    seeds = read_seed(pkg_name, dev)

    create_emulator(avd)
    x = start_idx
    while x < num_runs:
        info('------------- %d-th run ------------' % x)
        info('------------- %d-th run ------------' % x)
        info('------------- %d-th run ------------' % x)

        proc = run_on_emulator(apk, apk_name, x, ga_id, seeds, avd, dev,
                               num_nodes, fix_degree_per_node, window, True,
                               throttle, num_events)

        dbfile = '%s/db/%s_%s.db' % (MANI_DIR, pkg_name, dev)
        rdbfile = '%s/db/%s_%d_%s.random.db' % (MANI_DIR, pkg_name, x, dev)
        try:
            try:
                shutil.move(dbfile, rdbfile)
                print('%s >>> %s' % (dbfile, rdbfile))
            except:
                err('Error during moving %s to %s' % (dbfile, rdbfile))
                err('Redo %d-th run, first sleep for 30 seconds' % x)
                time.sleep(30)
                continue
            # out = subprocess.check_output(
            #     ['./read_db_graph_edge_histogram.py', rdbfile],
            #     stderr=subprocess.STDOUT,
            #     cwd=MANI_DIR,
            #     universal_newlines=True)
            # info('Read Database:\n%s' % out)
            # if '========' in out and 'Bad' in out:
            #     warn('Value beyond dictionary')
        except subprocess.CalledProcessError:
            err('Error read %s' % rdbfile)
            err('Redo %d-th run' % x)
            try:
                os.remove(dbfile)
                os.remove(rdbfile)
            except FileNotFoundError:
                pass
            continue
        finally:
            shutdown(avd, dev)
            kill_proc(proc)
            info('Kill emulator @%s' % proc.pid)

        x += 1


def read_seed(pkg, dev):
    seedfile = '%s/seed/%s.%s.seed' % (MANI_DIR, pkg, dev)
    if not os.path.isfile(seedfile):
        return None
    ret = []
    with open(seedfile) as f:
        for line in f:
            ret.append(line.strip())
    return ret


def create_emulator(avd='api_27'):
    try:
        out = subprocess.check_output(['./create_avd.sh', avd],
                                      stderr=subprocess.STDOUT,
                                      cwd=MANI_DIR,
                                      universal_newlines=True)
        if 'Do you wish to create a custom hardware profile? [no] no' in out:
            info('Emulator created...')
    except subprocess.CalledProcessError as e:
        err('Crash creating emulator: %s' % (e.output))
        msg = "Error: Android Virtual Device '%s' already exists." % avd
        if msg in e.output:
            warn('%s already exists' % avd)
            pass


def shutdown(avd, dev):
    avds = get_devices()
    info('Devices: %s' % avds)
    if not avds:
        return
    while dev in avds:
        warn('Shutting down %s at %s' % (avd, dev))
        subprocess.call([ADB, '-s', dev, 'shell', 'reboot', '-p'])
        time.sleep(10)
        avds = get_devices()
        if not avds:
            break


def run_on_emulator(apk, apk_name, x, ga_id, seeds, avd, dev, num_nodes,
                    fix_degree_per_node, window, reboot, throttle, num_events):
    if reboot:
        shutdown(avd, dev)
        port = dev[len('emulator-'):]
        cmd = [
            EMULATOR, '-avd', avd, '-verbose', '-wipe-data', '-no-audio',
            '-no-snapshot', '-port', port, '-no-boot-anim'
        ]
        if not window:
            cmd.append('-no-window')
        info(' '.join(cmd))
        proc = subprocess.Popen(cmd,
                                stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT,
                                universal_newlines=True)
        info('Running emulator @%s' % proc.pid)
    while True:
        inline = proc.stdout.readline()
        if not inline or proc.poll():
            # time.sleep(3)
            warn('AVD %s at %s @%s closed...' % (avd, dev, proc.pid))
            break
        progress('+')
        # sys.stdout.write(inline)
        if "emulator: ERROR: There's another emulator instance running with the current AVD" in inline:
            newline()
            warn('Multiple instances of %s at %s for %d-th run...' %
                 (avd, dev, x))
            kill_proc(proc)
            time.sleep(15)
            run_on_emulator(apk, apk_name, x, ga_id, seeds, avd, dev,
                            num_nodes, fix_degree_per_node, window, reboot,
                            throttle, num_events)
            break
        elif 'QXcbConnection: Could not connect to display' in inline:
            newline()
            warn('Failed to start avd %s at %s with window @%s' %
                 (avd, dev, proc.pid))
            warn('Restart without window...')
            kill_proc(proc)
            time.sleep(15)
            run_on_emulator(apk, apk_name, x, ga_id, seeds, avd, dev,
                            num_nodes, fix_degree_per_node, False, reboot,
                            throttle, num_events)
            break
        elif 'emulator: INFO: boot completed' in inline:
            newline()
            info('Emulator started...')
            instrument(apk, apk_name, ga_id)
            g = Gorilla(os.path.join(MANI_DIR, apk_name), device=dev)
            if not g.initialized:
                break
            g.disable_notif_bar()
            if not g.install_app():
                break
            g.clear_package()
            cur_total_degree = 0
            cur_total_screens = 0
            times = 0
            while cur_total_degree <= num_nodes * fix_degree_per_node or cur_total_screens < 2:
                if seeds and x < len(seeds):
                    g.run_monkey(throttle=throttle,
                                 num_events=num_events,
                                 seed=seeds[x])
                else:
                    g.run_monkey(throttle=throttle, num_events=num_events)
                time.sleep(5)
                info('Retrieving database...')
                g.store_dbs()
                time.sleep(5)
                pkg_name = apk_name[:-len('.apk')]
                dbfile = '%s/db/%s_%s.db' % (MANI_DIR, pkg_name, dev)
                try:
                    # hist = json.loads(gorilla.read_graph_edge_histogram_from_db(dbfile)[0])
                    # total_degree = sum(hist.values())
                    # total_degree = int(gorilla.read_number_of_events_from_db(dbfile)[0])
                    events = gorilla.read_actual_hits_from_db(dbfile)
                    screens = [json.loads(e[0])['&cd'] for e in events]
                    cur_total_screens = len(set(screens))
                    total_degree = int(
                        gorilla.read_actual_hit_num_from_db(dbfile)[0])
                    if total_degree == cur_total_degree:
                        g.force_stop()
                        time.sleep(5)
                    cur_total_degree = total_degree
                except:
                    err('Failed to read degree from %s' % dbfile)
                    os.remove(dbfile)
                times += 1
                warn('[%d] %d: Current degree: %d, #Screen: %d' %
                     (x, times, cur_total_degree, cur_total_screens))
                if times > 100:
                    warn('Too many (%s) Monkey tries, restart emulator...' %
                         times)
                    run_on_emulator(apk, apk_name, x, ga_id, seeds, avd, dev,
                                    num_nodes, fix_degree_per_node, window,
                                    reboot, throttle, num_events)
                    break
            break
    return proc


def read_xml():
    ret = {}
    for xml in glob.glob('%s/../../xml/*.xml' % MANI_DIR):
        pkg = xml.split('/')[-1][:-len('.apk.xml')]
        with open(xml) as fd:
            try:
                doc = xmltodict.parse(fd.read())
                ret[pkg] = doc['universe']
            except:
                pass
    return ret


def main():
    args = parse_args()
    apk_name = args.apk.split('/')[-1]

    xmls = read_xml()
    xml = xmls[apk_name[:-len('.apk')]]
    num_nodes = len(xml['name'])
    # num_nodes = sum(1 for line in open('%s/activities/%s.txt' % (MANI_DIR, apk_name)))
    info('----- number of nodes: %s' % num_nodes)
    run(args.apk, args.avd, args.device, args.ga_id, num_nodes,
        args.fix_degree_per_node, args.start_idx, args.num_runs, args.window,
        args.throttle, args.num_events)
    return


def parse_args():
    parser = argparse.ArgumentParser(description='Running emulators.')
    parser.add_argument('-v',
                        '--avd',
                        dest='avd',
                        metavar='AVD',
                        required=True,
                        help='specify AVD')
    parser.add_argument('-d',
                        '--device',
                        dest='device',
                        metavar='DEV',
                        required=True,
                        help='specify device name')
    parser.add_argument('-p',
                        '--apk',
                        dest='apk',
                        metavar='PATH',
                        required=True,
                        help='specify APK path')
    parser.add_argument('-i',
                        '--ga-id',
                        dest='ga_id',
                        metavar='ID',
                        default='UA-22467386-22',
                        help='specify custom GA ID')
    parser.add_argument('-g',
                        '--degree',
                        dest='fix_degree_per_node',
                        metavar='N',
                        type=int,
                        default=10,
                        help='specify degree per node')
    parser.add_argument('-s',
                        '--start-index',
                        dest='start_idx',
                        metavar='N',
                        type=int,
                        default=0,
                        help='specify start round')
    parser.add_argument('-n',
                        '--num-runs',
                        dest='num_runs',
                        metavar='N',
                        type=int,
                        default=1,
                        help='number of runs per emulator')
    parser.add_argument('-t',
                        '--throttle',
                        dest='throttle',
                        metavar='N',
                        type=int,
                        default=200,
                        help='throttle of Monkey, <1 for randomized throttle')
    parser.add_argument('-e',
                        '--events',
                        dest='num_events',
                        metavar='N',
                        type=int,
                        default=500,
                        help='number of events for each Monkey run')
    parser.add_argument('-w',
                        '--window',
                        dest='window',
                        action='store_true',
                        default=False,
                        help='show GUI')
    return parser.parse_args()


if __name__ == '__main__':
    main()
