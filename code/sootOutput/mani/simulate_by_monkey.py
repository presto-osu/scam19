#!/usr/bin/env python3

import os
import shutil
import signal
import subprocess
import sys
import time
from gorilla import info, warn, err, Gorilla, get_devices, progress, kill_proc
import gorilla

MANI_DIR = os.path.realpath(os.path.dirname(__file__))
GATOR_DIR = os.path.realpath(os.path.join(MANI_DIR, '..', '..'))
APK_DIR = os.path.realpath(
    os.path.join(GATOR_DIR, '..', '..', 'all-apk', 'play.20180422'))
GATOR = os.path.join(GATOR_DIR, 'gator')

hostname = subprocess.check_output(['hostname']).strip()

NUM_RUNS = 10
# SIMULATE_USERS = 1
EPSILON = 0.25


def newline():
    sys.stdout.write('\n')
    sys.stdout.flush()


def instrument(apk, apk_name, ga_id, random):
    if not os.path.isfile('%s/%s' % (MANI_DIR, apk_name)):
        warn('Instrument and sign...')
        cmd = '%s i -x xml/%s.xml -p %s --experiment --epsilon %s' % (
            GATOR, apk_name, apk, EPSILON)
        if ga_id:
            cmd = '%s --id %s' % (cmd, ga_id)
        if random != 'false':
            cmd = '%s --random' % cmd
        info(cmd)
        proc = subprocess.Popen(
            cmd.split(),
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
def run(apk, avd='api_27', dev='emulator-5554', ga_id=None,
        random='false', start_idx=0):
    apk_name = apk.split('/')[-1]
    info('---------------------- %s on %s -----------------' % (apk_name, dev))

    pkg_name = apk_name[:-len('.apk')]
    seeds = read_seed(pkg_name, dev)

    create_emulator(avd)
    x = start_idx
    while x < NUM_RUNS:
        info('------------- %d-th run ------------' % x)
        info('------------- %d-th run ------------' % x)
        info('------------- %d-th run ------------' % x)

        if hostname == b'yukon':
            proc = run_on_emulator(apk, apk_name, x, ga_id, random,
                    seeds, avd, dev, False)
        else:
            proc = run_on_emulator(apk, apk_name, x, ga_id, random,
                    seeds, avd, dev, False)

        dbfile = '%s/db/%s_%s.db' % (MANI_DIR, pkg_name, dev)
        rdbfile = '%s/db/%s_%d_%s.random.db' % (MANI_DIR,
                pkg_name, x, dev)
        try:
            if random != 'false':
                try:
                    shutil.move(dbfile, rdbfile)
                except:
                    err('Error during moving %s to %s' % (dbfile,
                        rdbfile))
                    err('Redo %d-th run' % x)
                    continue
                out = subprocess.check_output(
                    ['./read_db.py', rdbfile],
                    stderr=subprocess.STDOUT,
                    cwd=MANI_DIR,
                    universal_newlines=True)
                info('Read Database:\n%s' % out)
                if '========' in out and 'Bad' in out:
                    warn('Value beyond dictionary')
        except subprocess.CalledProcessError:
            err('Error read %s' % rdbfile)
            err('Redo %d-th run' % x)
            os.remove(dbfile)
            os.remove(rdbfile)
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
        out = subprocess.check_output(
            ['./create_avd.sh', avd],
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
        subprocess.call(['adb', '-s', dev, 'shell', 'reboot', '-p'])
        time.sleep(10)
        avds = get_devices()
        if not avds:
            break


def run_on_emulator(apk,
                    apk_name,
                    x,
                    ga_id,
                    random,
                    seeds,
                    avd='api_27',
                    dev='emulator-5554',
                    window=True,
                    reboot=True):
    if reboot:
        shutdown(avd, dev)
        port = dev[len('emulator-'):]
        cmd = [
            'emulator', '-avd', avd, '-verbose', '-wipe-data', '-no-audio',
            '-no-snapshot', '-port', port, '-no-boot-anim'
        ]
        if not window:
            cmd.append('-no-window')
        info(' '.join(cmd))
        proc = subprocess.Popen(
            cmd,
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
            warn('Multiple instances of %s at %s for %d-th run...' % (avd, dev,
                x))
            kill_proc(proc)
            time.sleep(15)
            run_on_emulator(apk, apk_name, x, ga_id, random, seeds, avd, dev,
                            window)
        if 'QXcbConnection: Could not connect to display' in inline:
            newline()
            warn('Failed to start avd %s at %s with window @%s' % (avd, dev,
                                                                   proc.pid))
            warn('Restart without window...')
            run_on_emulator(apk, apk_name, x, ga_id, random, seeds, avd, dev,
                            False)
        if 'emulator: INFO: boot completed' in inline:
            newline()
            info('Emulator started...')
            instrument(apk, apk_name, ga_id, random)
            g = Gorilla(os.path.join(MANI_DIR, apk_name), device=dev)
            if not g.initialized:
                break
            g.disable_notif_bar()
            if not g.install_app():
                break
            g.clear_package()
            num_hits = 0
            times = 0
            while num_hits < 100:
                if seeds and x < len(seeds):
                    g.run_monkey(num_events=500, seed=seeds[x])
                else:
                    g.run_monkey(num_events=500)
                time.sleep(5)
                info('Retrieving database...')
                g.store_dbs()
                time.sleep(5)
                pkg_name = apk_name[:-len('.apk')]
                dbfile = '%s/db/%s_%s.db' % (MANI_DIR, pkg_name, dev)
                try:
                    new_num_hits = int(gorilla.read_actual_hit_num_from_db(dbfile)[0])
                    if new_num_hits == num_hits:
                        g.force_stop()
                        time.sleep(5)
                    num_hits = new_num_hits
                except:
                    err('Failed to read number of hits from %s' %
                            dbfile)
                    os.remove(dbfile)
                times += 1
                warn('[%d] %d: Current number of hits: %d' % (x,
                    times, num_hits))
            break
            shutdown(avd, dev)

    return proc


def main():
    global NUM_RUNS
    if len(sys.argv) > 1:
        info(' '.join(sys.argv))
        apk = os.path.realpath(sys.argv[1])
        avd = sys.argv[2]
        dev = sys.argv[3]
        # ga_id = sys.argv[4]
        ga_id = 'UA-22467386-22'
        # random = sys.argv[5]
        random = 'true'
        try:
            NUM_RUNS = int(sys.argv[4])
        except:
            NUM_RUNS = 10
        try:
            start_idx = int(sys.argv[5])
        except:
            start_idx = 0
        run(apk, avd, dev, ga_id, random, start_idx)
        return


if __name__ == '__main__':
    main()
