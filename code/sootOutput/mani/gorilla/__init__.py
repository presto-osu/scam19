import concurrent.futures
import os
import re
import sqlite3
import subprocess
import sys
import time
import shutil
import datetime
from threading import Timer

try:
    from colorama import Fore

    GREEN = Fore.GREEN
    RED = Fore.RED
    MAGENTA = Fore.MAGENTA
    RESET = Fore.RESET
except:
    GREEN = ''
    RED = ''
    MAGENTA = ''
    RESET = ''

ADB = os.path.join(os.environ['ANDROID_SDK'], 'platform-tools', 'adb')

progress_signs = ['-', '/', '|', '\\']
progress_idx = 0


def progress(sign):
    global progress_idx
    progress_idx += 1
    progress_idx %= shutil.get_terminal_size().columns
    if progress_idx == 0:
        sys.stdout.write('\x1b[2K\r%s' % sign)
    else:
        sys.stdout.write(sign)
    sys.stdout.flush()


def current_milli_time():
    return int(round(time.time() * 1000))


def info(msg):
    print('%s [%sINFO%s] %s' % (datetime.datetime.now(), GREEN, RESET, msg))


def warn(msg):
    print('%s [%sWARN%s] %s' % (datetime.datetime.now(), MAGENTA, RESET, msg))


def err(msg):
    print('%s [%sERROR%s] %s' % (datetime.datetime.now(), RED, RESET, msg))


def kill_proc(proc):
    while proc and not proc.poll():
        info('Killing @%s...' % proc.pid)
        try:
            proc.terminate()
            proc.kill()
            os.kill(proc.pid, signal.SIGKILL)
            # os.killpg(os.getpgid(prev_proc.pid), signal.SIGKILL)
        except:
            warn('Error when killing @%s...' % proc.pid)
            break
        finally:
            time.sleep(3)


def get_devices():
    try:
        command = [ADB, 'devices']
        # info(' '.join(command))
        out = subprocess.check_output(command,
                                      stderr=subprocess.STDOUT,
                                      universal_newlines=True)
    except subprocess.CalledProcessError as e:
        err('Fail to run (%s): %s' % (e.cmd, e.output))
        return None
    out = out[len('List of devices attached') + 1:]
    tmp = out.split('\n')
    devices = []
    for d in tmp:
        if d == '':
            continue
        if not d.endswith('\tdevice'):
            continue
        devices.append(d[:-7])
    return devices


def get_current_app(device):
    cmd = ADB + ' -s %s shell dumpsys window w ' \
          '| grep \\/ | grep name= | sed "s/.*name=//g" ' \
          '| sed "s/)$//g"  | sed "s/\/.*$//g"' % device
    try:
        out = subprocess.check_output(cmd.split(),
                                      stderr=subprocess.STDOUT,
                                      universal_newlines=True)
    except subprocess.CalledProcessError as e:
        err('Fail to get package name (%s): %s' % (e.cmd, e.output))
        out = e.output


def read_screen_names_from_db(db_path):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute('SELECT name FROM screen_names')
    ret = c.fetchall()
    conn.close()
    return ret


def read_actual_hit_num_from_db(db_path):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute('SELECT total_actual_views FROM stats')
    ret = c.fetchone()
    conn.close()
    return ret


def read_actual_hits_from_db(db_path, limit=None):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    if not limit:
        c.execute('SELECT hit_map FROM hits ORDER BY hit_id ASC')
    else:
        c.execute('SELECT hit_map FROM hits ORDER BY hit_id ASC LIMIT %d' %
                  limit)
    ret = c.fetchall()
    conn.close()
    return ret


def read_actual_histogram_from_db(db_path):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute('SELECT actual_histogram FROM stats')
    ret = c.fetchone()
    conn.close()
    return ret


def read_random_hit_num_from_db(db_path):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute('SELECT total_random_views FROM stats')
    ret = c.fetchone()
    conn.close()
    return ret


def read_random_histogram_from_db(db_path):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute('SELECT random_histogram FROM stats')
    ret = c.fetchone()
    conn.close()
    return ret


class Gorilla:
    def __init__(self, apk_path, device=None, db_dir='db'):
        super(Gorilla, self).__init__()
        info('Run with:\nPython %s under %s' % (sys.version, os.getcwd()))
        self.apk_path = apk_path
        self.pkg_name = self.get_pkg_name()
        if not self.pkg_name:
            err('Cannot read package name of %s' % apk_path)
            self.initialized = False
            return
        self.db_dir = db_dir
        if not device:
            self.devices = self.getDevices()
        else:
            self.devices = [device]
        self.db_path = {}
        self.logcat_proc = {}
        self.logcat_reader_executor = concurrent.futures.ThreadPoolExecutor(
            max_workers=len(self.devices))
        self.monkey_proc = {}
        self.monkey_reader_executor = concurrent.futures.ThreadPoolExecutor(
            max_workers=len(self.devices))
        self.initialized = True

    def root(self):
        for device in self.devices:
            subprocess.call([ADB, '-s', device, 'root'])

    def get_pkg_name(self):
        command = 'aapt dump badging ' + self.apk_path + ' | grep "package: name"'
        try:
            out = subprocess.check_output(command.split(),
                                          stderr=subprocess.STDOUT,
                                          universal_newlines=True)
        except subprocess.CalledProcessError as e:
            err('Fail to get package name (%s): %s' % (e.cmd, e.output))
            out = e.output
        quoted = re.compile("'[^']*'")
        pkg = quoted.findall(out)
        if len(pkg) < 1:
            return
        if len(pkg[0]) < 2:
            return
        pkg_name = pkg[0][1:-1]
        return pkg_name

    def remove_app(self):
        if not self.initialized:
            return
        for device in self.devices:
            if not self.check_app_existence(device):
                warn(self.pkg_name + ' does not exist.')
                return
            command = ADB + ' -s ' + device + ' uninstall ' + self.pkg_name
            info(command)
            try:
                subprocess.call(command.split(),
                                stdout=subprocess.DEVNULL,
                                stderr=subprocess.STDOUT)
                info('Successfully remove %s' % self.pkg_name)
            except subprocess.CalledProcessError as e:
                err('Fail to remove app (%s): %s' % (e.cmd, e.output))

    def check_app_existence(self, device):
        command = ADB + ' -s ' + device + ' shell pm list packages -3'
        info(command)
        try:
            out = subprocess.check_output(command.split(),
                                          stderr=subprocess.STDOUT,
                                          universal_newlines=True)
            for line in out.split('\n'):
                # info(self.pkg_name + '..................' + line[len('package:'):])
                if line[len('package:'):] == self.pkg_name:
                    return True
        except subprocess.CalledProcessError as e:
            err('Fail to check existence (%s): %s' % (e.cmd, e.output))
        return False

    def install_app(self):
        for device in self.devices:
            if not self.install_app_per_device(device):
                return False
        return True

    def clear_package(self):
        for device in self.devices:
            self.clear_package_per_device(device)

    def clear_package_per_device(self, device):
        cmd = [ADB, '-s', device, 'shell', 'pm', 'clear', self.pkg_name]
        info(' '.join(cmd))
        subprocess.call(cmd)

    def install_app_per_device(self, device):
        if self.check_app_existence(device):
            return True
        try:
            command = [
                # ADB, '-s', device, 'shell', 'pm', 'install', '-g', self.apk_path
                ADB,
                '-s',
                device,
                'install',
                '-g',
                self.apk_path
            ]
            info(' '.join(command))
            out = subprocess.check_output(command,
                                          stderr=subprocess.STDOUT,
                                          timeout=120,
                                          universal_newlines=True)
            if 'Success' in out:
                return True
            err("Cannot install app <" + self.apk_path + "> on " + device)
            return False
        except subprocess.CalledProcessError as e:
            err("Error during installing app (%s): %s" % (e.cmd, e.output))
            return False
        except subprocess.TimeoutExpired as e:
            err("Timeout when installing app <" + self.apk_path + "> on " +
                device)
            return False

    def disable_notif_bar(self):
        for device in self.devices:
            self.disable_notif_bar_per_device(device)

    def enable_notif_bar(self):
        for device in self.devices:
            self.enable_notif_bar_per_device(device)

    def disable_notif_bar_per_device(self, device):
        self.root()
        command = ADB + ' -s %s shell settings put global policy_control immersive.full=*' % device
        info(command)
        subprocess.call(command.split())
        command = ADB + ' -s %s shell pm disable com.android.systemui' % device
        info(command)
        subprocess.call(command.split())

    def enable_notif_bar_per_device(self, device):
        self.root()
        command = ADB + ' -s %s shell settings put global policy_control null' % device
        info(command)
        subprocess.call(command.split())
        command = ADB + ' -s %s shell pm enable com.android.systemui' % device
        info(command)
        subprocess.call(command.split())

    def force_stop(self):
        for device in self.devices:
            self.force_stop_per_device(device)

    def force_stop_per_device(self, device):
        warn('Force stopping %s on %s' % (self.pkg_name, device))
        command = ADB + ' -s ' + device + ' shell am force-stop ' + self.pkg_name
        subprocess.call(command.split())

    def start_monkey(self, device, throttle, num_events, seed):
        command = ADB + ' -s %s shell monkey -p %s --pct-permission 0 --pct-appswitch 0 --pct-trackball 0 --pct-syskeys 0 --kill-process-after-error -v' % (
            device, self.pkg_name)
        if seed is not None:
            command = '%s -s %s' % (command, seed)
            info('Monkey with seed: %s' % seed)
        if throttle < 1:
            command = '%s --randomize-throttle' % command
            info('Monkey with randomized throttle')
        else:
            command = '%s --throttle %d' % (command, throttle)
        command = '%s %d' % (command, num_events)
        try:
            self.monkey_proc[device] = subprocess.Popen(
                command.split(),
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                universal_newlines=True)
            return self.monkey_proc[device]
        except subprocess.CalledProcessError as e:
            err('Crash running Monkey (%s): %s' % (e.cmd, e.output))
        return None

    def run_monkey(self, throttle=200, num_events=100, seed=None):
        if not self.initialized:
            return False
        # if not self.install_app():
        #     return False

        self.root()
        read_monkey_futures = {}
        read_logcat_futures = {}
        for device in self.devices:
            subprocess.call([ADB, '-s', device, 'logcat', '-c'])
            read_logcat_futures[device] = self.logcat_reader_executor.submit(
                self.read_logcat, device, self.start_logcat(device))
            read_monkey_futures[device] = self.monkey_reader_executor.submit(
                self.read_monkey, device,
                self.start_monkey(device, throttle, num_events, seed))
        ret = True
        for device, future in read_monkey_futures.items():
            ret = ret and future.result()
            self.kill_logcat(device)

        # for device, future in read_logcat_futures.items():
        #     future.result()

        return ret

    def start_logcat(self, device):
        subprocess.call([ADB, '-s', device, 'logcat', '-c'])
        try:
            self.logcat_proc[device] = subprocess.Popen(
                [ADB, '-s', device, 'logcat'],
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                universal_newlines=True)
            return self.logcat_proc[device]
        except subprocess.CalledProcessError as e:
            err('Crash running Logcat (%s): %s' % (e.cmd, e.output))
        return None

    def read_monkey(self, device, proc):
        info('Reading monkey on %s @%s' % (device, proc.pid))
        timer = Timer(60 * 5, proc.kill)
        try:
            timer.start()
            while True:
                inline = proc.stdout.readline()
                if not inline or proc.poll():
                    sys.stdout.write('\n')
                    info('Monkey finished on %s @%s' % (device, proc.pid))
                    timer.cancel()
                    return True
                # if 'Injection Failed' in inline:
                #     sys.stdout.write('\n')
                #     err('Monkey injection failed on %s @%s' % (device, proc.pid))
                #     time.sleep(5)
                #     # self.reboot(device)
                #     return False
                if 'Monkey aborted due to error.' in inline:
                    sys.stdout.write('\n')
                    err('Monkey aborted on %s @%s' % (device, proc.pid))
                    time.sleep(5)
                    # self.reboot(device)
                    timer.cancel()
                    return False
                # warn(inline)
                progress('.')
        finally:
            info('Monkey reader finished on %s @%s' % (device, proc.pid))
            timer.cancel()

    def kill_logcat(self, device):
        proc = self.logcat_proc[device]
        info('Kill logcat on %s @%s' % (device, proc.pid))
        # proc.terminate()
        proc.kill()
        # os.kill(proc.pid, signal.SIGKILL)

    def read_logcat(self, device, proc):
        info('Reading logcat on %s @%s' % (device, proc.pid))
        enq_timer = None
        glb_timer = time.time()
        while True:
            try:
                inline = proc.stdout.readline()
            except UnicodeDecodeError as ignore:
                continue
            if not inline or proc.poll():
                break
            if 'presto.ga.rt' not in inline:
                if enq_timer and time.time() - enq_timer > 60:
                    break
                if time.time() - glb_timer > 60 * 2:
                    break
                continue
            inline = re.sub(r'^[0-9\-:. A-Z]+ presto.ga.rt.[A-Za-z]+: ', '',
                            inline.strip())
            if 'Opening database' in inline:
                db = re.sub(r'Opening database: ', '', inline)
                sys.stdout.write('\n')
                info('Database: %s' % db)
                self.db_path[device] = db
            if 'Enqueue [' in inline:
                hit_id = re.search(r'\d+', inline)
                if hit_id:
                    enq_timer = time.time()
                    # sys.stdout.write(hit_id.group())
            if 'Hit saved to hits' in inline:
                progress(';')
            glb_timer = time.time()
            progress(':')
            # warn(inline)

    def store_dbs(self):
        for device in self.devices:
            self.store_db(device)

    def store_db(self, device):
        # info('Going to pull %s' % db_path)
        try:
            db_path = self.db_path[device]
        except:
            return
        if not db_path:
            return
        cmd = [
            ADB, '-s', device, 'pull', db_path,
            '%s/%s_%s.db' % (self.db_dir, self.pkg_name, device)
        ]
        info(' '.join(cmd))
        # os.system(' '.join(cmd))
        subprocess.call(cmd, stderr=subprocess.STDOUT, universal_newlines=True)
        # info('Finish pulling %s' % db_path)

    def reboot(self):
        for device in self.devices:
            self.reboot_one_device(device)

    def reboot_one_device(self, device):
        cmd = [ADB, '-s', device, 'reboot']
        err('Going to reboot %s' % device)
        subprocess.call(cmd)
        cmd = [ADB, '-s', device, 'wait-for-device']
        subprocess.call(cmd)
        self.root()
        info(self.getDevices())
        time.sleep(30)

    def read_screen_names(self):
        for device in self.devices:
            db_path = '%s/%s_%s.db' % (self.db_dir, self.pkg_name, device)
            conn = sqlite3.connect(db_path)
            c = conn.cursor()
            c.execute('SELECT name FROM screen_names')
            info('Screen names: %s' % c.fetchall())
            conn.close()
