#!/usr/bin/env python3

import glob
import gorilla
import json
import math
import os
import sys
import xmltodict
import time
from collections import Counter, OrderedDict, defaultdict
from colorama import Fore
import matplotlib as mpl
if os.environ.get('DISPLAY', '') == '':
    print('No display found. Using non-interactive Agg backend')
    mpl.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import numpy as np
import scipy.stats as st

SIM_DIR = os.path.realpath(os.path.dirname(__file__))

EPSILON = [math.log(9), math.log(49)]
EPSILON_LABEL = {
    math.log(9): r'$\epsilon=\ln(9)$',
    math.log(49): r'$\epsilon=\ln(49)$'
}
# EPSILON = [0.2]
# EPSILON_LABEL = {
#     0.2: r'$\epsilon=0.2$',
# }

def read_xml():
    ret = {}
    for xml in glob.glob('%s/../xml/*.xml' % SIM_DIR):
        pkg = xml.split('/')[-1][:-len('.apk.xml')]
        with open(xml) as fd:
            try:
                doc = xmltodict.parse(fd.read())
                ret[pkg] = doc['universe']
            except:
                pass
    return ret


def main():
    xmls = read_xml()
    sim_user = 1
    t = -1
    k = 100
    if len(sys.argv) > 1:
        sim_user = int(sys.argv[1])
    if len(sys.argv) > 2:
        t = int(sys.argv[2])
    plot = True
    dbs = glob.glob('%s/../db/*random.db' % SIM_DIR)
    # dbs.sort(key=os.path.getmtime)

    pkg2db = {}
    pkg2user2trace = {}
    pkg2user2rtrace = {}
    for db in dbs:
        emulator = db.split('/')[-1][:-len('.random.db')]
        emulator = emulator.split('_')[-1]
        pkg = db.split('/')[-1][:-len('_emulator-xxxx.random.db')]
        user = pkg.split('_')[-1] + '_' + emulator
        pkg = pkg.split('_')[:-1]
        pkg = '_'.join(pkg)
        dbss = pkg2db[pkg] if pkg in pkg2db else set()
        dbss.add(db)
        pkg2db[pkg] = dbss
        if pkg not in pkg2user2trace:
            pkg2user2trace[pkg] = {}
        pkg2user2trace[pkg][user] = []
        if pkg not in pkg2user2rtrace:
            pkg2user2rtrace[pkg] = {}
        pkg2user2rtrace[pkg][user] = []

    pkg2actualhist = {}
    pkg2hist = {}
    pkg2rhist = {}
    pkg2num = {}
    pkg2rnum = {}
    pkg2xname = {}
    pkg2name = {}

    ep2user2pkg2l_inf = {}
    ep2user2pkg2overhead = {}
    ep2user2pkg2overhead_ratio = {}
    for ep in EPSILON:
        ep2user2pkg2l_inf[ep] = {}
        ep2user2pkg2overhead[ep] = {}
        ep2user2pkg2overhead_ratio[ep] = {}

    for pkg in sorted(pkg2db):
        print('====== %s:' % pkg)
        print('====== #db=%d' % len(pkg2db[pkg]))

        xml = xmls[pkg]
        if xml['autoTracking'] == 'true' or 'name' not in xml:
            continue

        pkg2num[pkg] = 0
        pkg2rnum[pkg] = {}
        pkg2actualhist[pkg] = {}
        pkg2hist[pkg] = {}
        pkg2rhist[pkg] = {}
        pkg2xname[pkg] = set()
        pkg2name[pkg] = set()

        for name in xml['name']:
            if not name:
                name = ''
            pkg2xname[pkg].add(name)
            pkg2actualhist[pkg][name] = 0
            pkg2hist[pkg][name] = 0

        # read real hits
        for db in pkg2db[pkg]:
            emulator = db.split('/')[-1][:-len('.random.db')]
            emulator = emulator.split('_')[-1]
            tmp = db.split('/')[-1][:-len('_emulator-xxxx.random.db')]
            user = tmp.split('_')[-1] + '_' + emulator
            # print('[%s] user-%s' % (pkg, user))
            try:
                if k != -1:
                    events = gorilla.read_actual_hits_from_db(db, k)
                else:
                    events = gorilla.read_actual_hits_from_db(db)
                # print('\tevents=%d' % len(events))
            except:
                print('Error reading hits from %s' % db)
                continue
                # os.remove(db)
            for event in events:
                event = json.loads(event[0])
                name = event['&cd']
                pkg2user2trace[pkg][user].append(name)
                pkg2name[pkg].add(name)

    users = 1
    while users <= sim_user:
        for pkg in sorted(pkg2xname, key=lambda k: len(pkg2xname[k])):
            for name in pkg2hist[pkg]:
                pkg2hist[pkg][name] = 0
                pkg2actualhist[pkg][name] = 0
            # sampling
            samples = {}
            u = 0
            for user in sorted(pkg2user2trace[pkg]):
                for name in pkg2user2trace[pkg][user]:
                    pkg2actualhist[pkg][name] += users
                samples[user] = sampling(pkg, u, pkg2user2trace[pkg][user],
                                         t, k)
                u += 1

            # print('............... %s' % samples)

            for user in samples:
                for name in samples[user]:
                    pkg2hist[pkg][name] += users
            pkg2num[pkg] = sum(pkg2hist[pkg].values())
            # title = '%s\nk=%d, t=%d' % (pkg, k, t)
            # print_2_hist(pkg, title, pkg2actualhist[pkg],
            #              {n: int(v * k / t)
            #               for n, v in pkg2hist[pkg].items()})

            # start simulate randomization
            for epsilon in EPSILON:
                if users not in ep2user2pkg2overhead[epsilon]:
                    ep2user2pkg2overhead[epsilon][users] = OrderedDict()
                ep2user2pkg2overhead[epsilon][users][pkg] = []
                if users not in ep2user2pkg2overhead_ratio[epsilon]:
                    ep2user2pkg2overhead_ratio[epsilon][users] = OrderedDict()
                ep2user2pkg2overhead_ratio[epsilon][users][pkg] = []
                if users not in ep2user2pkg2l_inf[epsilon]:
                    ep2user2pkg2l_inf[epsilon][users] = OrderedDict()
                ep2user2pkg2l_inf[epsilon][users][pkg] = []
                l_inf = []
                l_inf_e = []
                # 20 trials
                for idx in range(20):
                    pkg2rhist[pkg][epsilon] = {}
                    for user in sorted(samples):
                        rhist = randomize(idx, epsilon, samples[user],
                                          pkg2xname[pkg], users)
                        pkg2rhist[pkg][epsilon] = dsum(pkg2rhist[pkg][epsilon],
                                                       rhist)
                    pkg2rnum[pkg][epsilon] = sum(
                        pkg2rhist[pkg][epsilon].values())

                    print('[%2d][%d] %s ---------------------------' %
                          (idx, users, pkg))
                    xnames = pkg2xname[pkg]
                    ep = epsilon
                    print('\tepsilon=%s' % ep)
                    num_actual_events = sum(pkg2actualhist[pkg].values())
                    print('\t\t%sGood%s: %s/%s '
                          '#actual_hits=%s(%s) #random_hits=%s' %
                          (Fore.GREEN, Fore.RESET, len(pkg2name[pkg]),
                           len(xnames), pkg2num[pkg], num_actual_events,
                           pkg2rnum[pkg][ep]))
                    l_infs = norm(pkg, pkg2hist[pkg], pkg2rhist[pkg][ep],
                                  pkg2num[pkg], pkg2rnum[pkg][ep],
                                  len(samples) * users, float(ep),
                                  False)  #plot)
                    l_inf.append(l_infs[0])
                    l_inf_e.append(l_infs[1])
                    ep2user2pkg2l_inf[epsilon][users][pkg].append(l_infs[0])
                    ep2user2pkg2overhead[epsilon][users][pkg].append(
                        (pkg2rnum[pkg][ep] - num_actual_events) / num_actual_events)
                    ep2user2pkg2overhead_ratio[epsilon][users][pkg].append(
                        pkg2rnum[pkg][ep] / num_actual_events)
                print(
                    '------ %s: #users=%d, epsilon=%.3f, mean(l_inf/f) = %.3f'
                    % (pkg, users * 100, epsilon, np.mean(l_inf)))
                # print('------ %s max(l_inf/f)=%f'
                #     % (pkg, max(l_inf)))
                # print('------ %s mean(l_inf/f)=%f'
                #     % (pkg, np.mean(l_inf)))

                # print('------ %s med(l_inf_e/f)=%f'
                #     % (pkg, np.median(l_inf_e)))
                # print('------ %s max(l_inf_e/f)=%f'
                #     % (pkg, max(l_inf_e)))
                # print('------ %s mean(l_inf_e/f)=%f'
                #     % (pkg, np.mean(l_inf_e)))
        users *= 10

    plot_acc(ep2user2pkg2l_inf, ep2user2pkg2overhead,
            ep2user2pkg2overhead_ratio,
             sorted(pkg2xname, key=lambda k: len(pkg2xname[k])), t)
    # plot_acc_3d(ep2user2pkg2l_inf,
    #          sorted(pkg2xname, key=lambda k: len(pkg2xname[k])))
    return


def plot_acc(ep2user2pkg2l_inf, ep2user2pkg2overhead,
        ep2user2pkg2overhead_ratio, pkgs, t=-1):
    # mpl.rcParams['text.usetex'] = True

    # print('::::: %s' % ep2user2pkg2l_inf)
    # colors = [str(0.7 - 0.2 * i) for i in range(len(ep2user2pkg2l_inf))]
    ind = np.arange(len(pkgs))  # the x locations for the groups
    bar_width = 0.3
    opacity = 1

    size = plt.rcParams["figure.figsize"]
    size[1] = size[1] / 2
    fig_oh = plt.figure(figsize=size)
    ax_oh = fig_oh.add_subplot(111)
    colors = [str(0.8 - 0.3 * i) for i in
            range(len(ep2user2pkg2overhead))]
    j = 0
    users = 0
    for ep, c in zip(ep2user2pkg2overhead, colors):
        user2pkg2overhead = ep2user2pkg2overhead[ep]
        user2pkg2overhead_ratio = ep2user2pkg2overhead_ratio[ep]
        u = sorted(user2pkg2overhead)[-1]
        users = u
        pkg2overhead = ep2user2pkg2overhead[ep][u]
        pkg2overhead_ratio = ep2user2pkg2overhead_ratio[ep][u]

        i = [
            i - bar_width * int(len(ep2user2pkg2overhead) / 2) + bar_width * j
            for i in ind
        ]
        j += 1

        # overhead
        data = [np.mean(l) for l in pkg2overhead.values()]
        data_ratio = [np.mean(l) for l in pkg2overhead_ratio.values()]
        print('!!! overhead-dat [%s] %s' % (EPSILON_LABEL[ep], data))
        print('!!! overhead-dat-ratio [%s] %s' % (EPSILON_LABEL[ep], data_ratio))
        confidence = 0.95
        err = [
            st.sem(l) * st.t.ppf((1 + confidence) / 2.,
                                 len(l) - 1)
            for l in pkg2overhead.values()
        ]
        err_ratio = [
            st.sem(l) * st.t.ppf((1 + confidence) / 2.,
                                 len(l) - 1)
            for l in pkg2overhead_ratio.values()
        ]
        print('!!! overhead-err [%s] %s' % (EPSILON_LABEL[ep], err))
        print('!!! overhead-err-ratio [%s] %s' % (EPSILON_LABEL[ep], err_ratio))

        # ax_oh.plot(i, data, '-', color=c,
        #     label='#users=%d' % (u * 100))  #, markersize=3)
        # ax_oh.plot(i, data, 'k.', markersize=3)

        cs = [c] * len(data)
        ax_oh.bar(
                i,
                data_ratio,
                bar_width,
                alpha=opacity,
                color=cs,
                # yerr = err,
                label='%s' % EPSILON_LABEL[ep])
        # ax_oh.errorbar(
        #     i, data, yerr=err, color='0', ls='none', lw=0.5, capsize=2)

    #ax_oh.set_ylabel('overhead')
    ax_oh.set_ylabel('#sent-events/#real-events')
    ax_oh.legend(
        loc='upper left', ncol=len(user2pkg2overhead), fontsize='small')
    if t != -1:
        title = '%s, t=%d, k=100, #users=%d' % (EPSILON_LABEL[ep], t,
                users * 100)
    else:
        title = '%s, k=100, #users=%d' % (EPSILON_LABEL[ep], users *
                100)
    ax_oh.set_title(title)
    ax_oh.set_xticks(ind)
    ax_oh.set_xticklabels(
        [
            'SpeedLogic', 'ParKing', 'DPM', 'Barometer', 'LocTracker',
            'Vidanta', 'MoonPhases', 'DailyBible', 'DrumPads', 'QuickNews',
            'Posts', 'Mitula', 'KFOR', 'Equibase', 'Parrot'
        ],
        rotation=30,
        # rotation_mode='anchor',
        size='x-small')
    fig_oh.tight_layout()
    if t == -1:
        filename = 'figure/overhead-wo-samp.pdf'
    else:
        filename = 'figure/overhead-t-%d.pdf' % (t)
    fig_oh.savefig(filename, bbox_inches='tight')


    size = plt.rcParams["figure.figsize"]
    size[1] = size[1] * 2
    fig = plt.figure(figsize=size)
    pos = 100 * len(ep2user2pkg2l_inf) + 11
    for ep in ep2user2pkg2l_inf:
        ax = fig.add_subplot(pos)
        # ax_oh = fig_oh.add_subplot(pos)
        pos += 1
        print('::::: epsilon=%f' % ep)
        user2pkg2l_inf = ep2user2pkg2l_inf[ep]
        j = 0
        colors = [str(0.8 - 0.2 * i) for i in range(len(user2pkg2l_inf))]
        for u, c in zip(user2pkg2l_inf, colors):
            pkg2l_inf = user2pkg2l_inf[u]
            # pkg2overhead = ep2user2pkg2overhead[ep][u]

            i = [
                i - bar_width * int(len(user2pkg2l_inf) / 2) + bar_width * j
                for i in ind
            ]
            j += 1

            # accuracy
            # print('\t::::: #users=%d' % (u * 100))
            data = [np.mean(l) for l in pkg2l_inf.values()]
            print('!!! accuracy-dat [%s] [%s] %s' % (u, EPSILON_LABEL[ep], data))
            # print('\t::::: mean=%s' % data)
            confidence = 0.95
            err = [
                st.sem(l) * st.t.ppf((1 + confidence) / 2.,
                                     len(l) - 1) for l in pkg2l_inf.values()
            ]
            print('!!! accuracy-err [%s] [%s] %s' % (u, EPSILON_LABEL[ep], err))
            # print('\t::::: err=%s' % err)
            cs = [c] * len(data)
            ax.bar(
                i,
                data,
                bar_width,
                alpha=opacity,
                color=cs,
                # yerr = err,
                label='#users=%d' % (u * 100))
            ax.errorbar(
                i, data, yerr=err, color='0', ls='none', lw=0.5, capsize=2)

            # overhead
            # data = [np.mean(l) for l in pkg2overhead.values()]
            # confidence = 0.95
            # err = [
            #     st.sem(l) * st.t.ppf((1 + confidence) / 2.,
            #                          len(l) - 1)
            #     for l in pkg2overhead.values()
            # ]
            # ax_oh.plot(i, data, '-', color=c,
            #     label='#users=%d' % (u * 100))  #, markersize=3)
            # ax_oh.plot(i, data, 'k.', markersize=3)
            # ax_oh.errorbar(
            #     i, data, yerr=err, color='0', ls='none', lw=0.5, capsize=2)


        ax.set_ylabel('max error')
        ax.legend(
            loc='upper left', ncol=len(user2pkg2l_inf), fontsize='small')
        if t != -1:
            title = '%s, t=%d, k=100' % (EPSILON_LABEL[ep], t)
        else:
            title = '%s, k=100' % EPSILON_LABEL[ep]
        ax.set_title(title)
        ax.set_xticks(ind)
        ax.set_xticklabels(
            [
                'SpeedLogic', 'ParKing', 'DPM', 'Barometer', 'LocTracker',
                'Vidanta', 'MoonPhases', 'DailyBible', 'DrumPads', 'QuickNews',
                'Posts', 'Mitula', 'KFOR', 'Equibase', 'Parrot'
            ],
            rotation=30,
            # rotation_mode='anchor',
            size='x-small')

        # ax_oh.set_ylabel('overhead')
        # ax_oh.legend(
        #     loc='upper left', ncol=len(user2pkg2l_inf), fontsize='small')
        # ax_oh.set_title(title)
        # ax_oh.set_xticks(ind)
        # ax_oh.set_xticklabels(
        #     [
        #         'SpeedLogic', 'ParKing', 'DPM', 'Barometer', 'LocTracker',
        #         'Vidanta', 'MoonPhases', 'DailyBible', 'DrumPads', 'QuickNews',
        #         'Posts', 'Mitula', 'KFOR', 'Equibase', 'Parrot'
        #     ],
        #     rotation=30,
        #     # rotation_mode='anchor',
        #     size='x-small')

    fig.tight_layout()
    if t == -1:
        filename = 'figure/accuracy-wo-samp.pdf'
    else:
        filename = 'figure/accuracy-t-%d.pdf' % (t)
    fig.savefig(filename, bbox_inches='tight')

    # fig_oh.tight_layout()
    # if t == -1:
    #     filename = 'figure/overhead-wo-samp.pdf'
    # else:
    #     filename = 'figure/overhead-t-%d.pdf' % (t)
    # fig_oh.savefig(filename, bbox_inches='tight')
    plt.clf()
    plt.cla()
    plt.close()


def plot_acc_3d(ep2user2pkg2l_inf, pkgs):
    # mpl.rcParams['text.usetex'] = True
    ind = np.arange(len(pkgs))  # the x locations for the groups
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    bar_width = 0.2
    for ep in ep2user2pkg2l_inf:
        user2pkg2l_inf = ep2user2pkg2l_inf[ep]
        j = 0
        for u, c in zip(user2pkg2l_inf, ('r', 'g', 'b')):
            pkg2l_inf = user2pkg2l_inf[u]

            i = [
                i - bar_width * int(len(user2pkg2l_inf) / 2) + bar_width * j
                for i in ind
            ]
            j += 1

            data = [np.mean(l) for l in pkg2l_inf.values()]
            z = 1.96
            err = [
                z * np.std(l) / math.sqrt(len(l)) for l in pkg2l_inf.values()
            ]

            # You can provide either a single color or an
            # array. To demonstrate this,
            # the first bar of each set will be colored
            # cyan.
            cs = [c] * len(pkgs)
            ax.bar(i, data, zs=ep, zdir='y', color=cs, alpha=0.8)
            # ax.errorbar(i, data, yerr = err,
            #         color='0',
            #         ls='none',
            #         lw=0.5, capsize=1)

    ax.set_xticks([])
    # ax.set_xticklabels(pkgs,
    #         rotation=45,
    #         # rotation_mode='anchor',
    #         size='xx-small')

    ax.set_xlabel('Apps')
    ax.set_ylabel('Epsilon')
    ax.set_zlabel(
        r'$\max_{v\in\mathcal{D}}{\frac{|\hat{f}(v)-f(v)|}{f}}$', usetex=True)

    plt.savefig('figure/accuracy-3d.pdf', bbox_inches='tight')
    plt.clf()
    plt.cla()
    plt.close()

    pass


def sampling(pkg, seed, trace, t, k):
    if not seed:
        seed = int(time.time())
    prng = np.random.RandomState(seed)

    # sampling trace
    if t != -1:
        if len(trace) < t:
            rn = list(range(0, len(trace)))
        else:
            rn = prng.choice(range(0, min(k, len(trace))), t, False)
        newtrace = [trace[i] for i in rn]
        # print('[%s] Trace: %d -> %d' % (pkg, len(trace), len(newtrace)))
        trace = newtrace
    return trace


def randomize(seed, epsilon, trace, dictionary, sim_user):
    if not seed:
        seed = int(time.time())
    prng = np.random.RandomState(seed)

    rhist = {v: 0 for v in dictionary}
    this_prob = math.exp(epsilon / 2) / (1 + math.exp(epsilon / 2))
    others_prob = 1 / (1 + math.exp(epsilon / 2))
    for user in range(0, sim_user):
        for v in trace:
            for i in sorted(dictionary):
                if i == v:
                    if prng.uniform() <= this_prob:
                        rhist[i] += 1
                elif prng.uniform() <= others_prob:
                    rhist[i] += 1
    # print('*** epsilon=%s, this_prob=%s, others_prob=%s' %
    # (epsilon, this_prob, others_prob))
    # print('*** actual histogram: %s' % Counter(trace))
    # print('*** random histogram: %s' % rhist)
    return rhist


def norm(pkg, hist, rhist, n, rn, users, epsilon, plot=False):
    estimate1 = {}
    for name, freq in rhist.items():
        estimate1[name] = math.floor(((1 + math.exp(epsilon / 2)) * freq - n) /
                                     (math.exp(epsilon / 2) - 1))
        if estimate1[name] < 0:
            estimate1[name] = 0
    l_one = 0
    l_inf = 0
    for name in hist:
        distance = abs(hist[name] - estimate1[name])
        # print('dist=|%s-%s|=%s' % (estimate[name], hist[name], distance))
        l_inf = max(distance, l_inf)
        l_one += distance
    if plot:
        print('\t\t%sNorm (f_actual)%s: L_one=%d L_inf=%d L_inf/n=%f' %
              (Fore.YELLOW, Fore.RESET, l_one, l_inf, l_inf / n))

    # p = math.exp(epsilon / 2) / (1 + math.exp(epsilon / 2))
    # sc = (1 - p) / (p + (len(hist) - 1) * (1 - p))
    # estimate = {}
    # for name, freq in rhist.items():
    #     estimate[name] = math.floor((freq - sc * sum(rhist.values()))/(2 * p - 1))
    f = math.ceil((1 + math.exp(epsilon / 2)) * sum(rhist.values()) /
                  (math.exp(epsilon / 2) + len(hist) - 1))
    estimate2 = {}
    for name, freq in rhist.items():
        estimate2[name] = math.floor(((1 + math.exp(epsilon / 2)) * freq - f) /
                                     (math.exp(epsilon / 2) - 1))
        if estimate2[name] < 0:
            estimate2[name] = 0
    l_one_e = 0
    l_inf_e = 0
    for name in hist:
        distance = abs(hist[name] - estimate2[name])
        # print('dist=|%s-%s|=%s' % (estimate[name], hist[name], distance))
        l_inf_e = max(distance, l_inf)
        l_one_e += distance
    if plot:
        print('\t\t%sNorm (f_estimate)%s: L_one=%d L_inf=%d L_inf/f=%f' %
              (Fore.YELLOW, Fore.RESET, l_one_e, l_inf_e, l_inf_e / f))
        print('\t\t%sf_actual - f_estimate = %s %s' % (Fore.YELLOW, Fore.RESET,
                                                       n - f))

    if plot:
        ind = np.arange(len(hist))  # the x locations for the groups
        bar_width = 0.2
        opacity = 1

        names = sorted(hist)
        real = [hist[k] for k in sorted(hist)]
        est1 = [estimate1[k] for k in sorted(estimate1)]
        est2 = [estimate2[k] for k in sorted(estimate2)]

        # print('++++++++++++++ %s' % ind)
        # print('++++++++++++++ %s' % names)
        # print('++++++++++++++ %s' % real)
        # print('++++++++++++++ %s' % est1)
        # print('++++++++++++++ %s' % est2)

        p1 = plt.bar(
            ind - bar_width, real, bar_width, alpha=opacity, color='pink')
        p2 = plt.bar(ind, est1, bar_width, alpha=opacity, color='lightblue')
        # p3 = plt.bar(
        #     ind + bar_width,
        #     est2,
        #     bar_width,
        #     alpha=opacity,
        #     color='lightgreen')

        cell_text = []
        cell_text.append([x for x in real])
        cell_text.append([x for x in est1])
        cell_text.append([abs(est1[i] - real[i]) for i in ind])
        cell_text.append(['%.4f' % (abs(est1[i] - real[i]) / n) for i in ind])
        # cell_text.append([x for x in est2])
        the_table = plt.table(
            cellText=cell_text,
            cellLoc='center',
            colLabels=names,
            rowLabels=[
                'Real', 'Estimate\n(actual#events)', 'Delta', 'Delta / n'
                # 'Estimate\n(estimated#events)'
            ],
            rowColours=['pink', 'lightblue', 'white', 'white'],
            # 'lightgreen'],
            rowLoc='right',
        )
        plt.ylabel('#Hits')
        plt.xticks([])
        plt.title(pkg + ('\nepsilon=%.2f' % epsilon) + ', #real_events=' +
                  str(n) + ', #random_events=' + str(rn) + '\n#users=' +
                  str(users) + ', L_infinity=' + str(l_inf) +
                  (', L_infinity/n=%.4f' % (l_inf / n)))
        plt.savefig(
            'figure/%s-epsilon-%.2f-users-%d.pdf' % (pkg, epsilon, users),
            bbox_inches='tight')
        plt.clf()
        plt.cla()
        plt.close()

    return (l_inf / n, l_inf_e / f)


def print_2_hist(pkg, title, hist, other_hist):
    ind = np.arange(len(hist))  # the x locations for the groups
    bar_width = 0.2
    opacity = 1

    names = sorted(hist)
    one = [hist[k] for k in sorted(hist)]
    two = [other_hist[k] for k in sorted(hist)]
    p1 = plt.bar(
        ind - bar_width / 2, one, bar_width, alpha=opacity, color='pink')
    p2 = plt.bar(
        ind + bar_width / 2, two, bar_width, alpha=opacity, color='lightblue')

    cell_text = []
    cell_text.append([x for x in one])
    cell_text.append([x for x in two])
    cell_text.append([abs(one[i] - two[i]) for i in ind])
    n = sum(hist.values())
    cell_text.append(['%.4f' % (abs(one[i] - two[i]) / n) for i in ind])
    the_table = plt.table(
        cellText=cell_text,
        cellLoc='center',
        colLabels=names,
        rowLabels=['Actual', 'Sampling*k/t', 'Delta', 'Delta / n'],
        rowColours=['pink', 'lightblue', 'white', 'white'],
        rowLoc='right',
    )
    plt.ylabel('#Hits')
    plt.xticks([])
    plt.title(title)
    plt.savefig('figure/%s-sampling.pdf' % (pkg), bbox_inches='tight')
    plt.clf()
    plt.cla()
    plt.close()


def dsum(*dicts):
    ret = defaultdict(int)
    for d in dicts:
        for k, v in d.items():
            ret[k] += v
    return dict(ret)


if __name__ == '__main__':
    main()
