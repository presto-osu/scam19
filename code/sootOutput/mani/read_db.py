#!/usr/bin/env python

import glob
import gorilla
import json
import math
import os
import sys
import xmltodict
from colorama import Fore
# import matplotlib as mpl
# if os.environ.get('DISPLAY', '') == '':
#     print('No display found. Using non-interactive Agg backend')
#     mpl.use('Agg')
# import matplotlib.pyplot as plt
# import numpy as np

MANI_DIR = os.path.realpath(os.path.dirname(__file__))


def norm(pkg, hist, rhist, epsilon, plot=False):
    n = sum(hist.values())
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
    print('\t\t%sNorm (f_actual)%s: L_one=%d L_inf=%d' %
          (Fore.YELLOW, Fore.RESET, l_one, l_inf))

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
    l_one = 0
    l_inf = 0
    for name in hist:
        distance = abs(hist[name] - estimate2[name])
        # print('dist=|%s-%s|=%s' % (estimate[name], hist[name], distance))
        l_inf = max(distance, l_inf)
        l_one += distance
    print('\t\t%sNorm (f_estimate)%s: L_one=%d L_inf=%d' %
          (Fore.YELLOW, Fore.RESET, l_one, l_inf))
    print('\t\t%sf_actual - f_estimate = %s %s' % (Fore.YELLOW, Fore.RESET,
                                                   n - f))

    # if plot:
    #     ind = np.arange(len(hist))  # the x locations for the groups
    #     bar_width = 0.2
    #     opacity = 1

    #     real = [hist[k] for k in sorted(hist)]
    #     est1 = [estimate1[k] for k in sorted(estimate1)]
    #     est2 = [estimate2[k] for k in sorted(estimate2)]
    #     p1 = plt.bar(
    #         ind - bar_width, real, bar_width, alpha=opacity, color='pink')
    #     p2 = plt.bar(ind, est1, bar_width, alpha=opacity, color='lightblue')
    #     p3 = plt.bar(
    #         ind + bar_width,
    #         est2,
    #         bar_width,
    #         alpha=opacity,
    #         color='lightgreen')

    #     names = sorted(hist)
    #     cell_text = []
    #     cell_text.append([x for x in real])
    #     cell_text.append([x for x in est1])
    #     cell_text.append([x for x in est2])
    #     the_table = plt.table(
    #         cellText=cell_text,
    #         cellLoc='center',
    #         colLabels=names,
    #         rowLabels=[
    #             'Real', 'Estimate\n(actual#events)',
    #             'Estimate\n(estimated#events)'
    #         ],
    #         rowColours=['pink', 'lightblue', 'lightgreen'],
    #         rowLoc='right',
    #     )
    #     plt.ylabel('#Hits')
    #     plt.xticks([])
    #     plt.title(pkg + ', epsilon=' + str(epsilon))
    #     plt.savefig(
    #         'figure/' + pkg + '-epsilon-' + str(epsilon) + '.pdf',
    #         bbox_inches='tight')


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
    xmls = read_xml()
    plot = False
    if len(sys.argv) > 1:
        dbs = sys.argv[1:]
    else:
        plot = True
        dbs = glob.glob('%s/db/*.db' % MANI_DIR)
        dbs.sort(key=os.path.getmtime)
    pkg2db = {}
    for db in dbs:
        if db.endswith('random.db') or db.endswith('actual.db'):
            pkg = db.split('/')[-1][:-len('_emulator-xxxx.yyyyyy.db')]
            pkg = pkg.split('_')[:-1]
            pkg = '_'.join(pkg)
        else:
            pkg = db.split('/')[-1][:-len('_emulator-xxxx.db')]
        dbs = pkg2db[pkg] if pkg in pkg2db else set()
        dbs.add(db)
        pkg2db[pkg] = dbs

    pkg2hit = {}
    pkg2hist = {}
    pkg2rhit = {}
    pkg2rhist = {}
    pkg2xname = {}
    pkg2name = {}

    for pkg in pkg2db:
        xml = xmls[pkg]
        if xml['autoTracking'] == 'true' or 'name' not in xml:
            continue

        pkg2hit[pkg] = 0
        pkg2hist[pkg] = {}
        pkg2rhit[pkg] = {}
        pkg2rhist[pkg] = {}
        pkg2xname[pkg] = set()
        pkg2name[pkg] = set()

        for name in xml['name']:
            if not name:
                name = ''
            pkg2xname[pkg].add(name)
            pkg2hist[pkg][name] = 0

        for db in pkg2db[pkg]:
            # print(gorilla.read_actual_hits_from_db(db))

            try:
                user2views = json.loads(
                    gorilla.read_random_hit_num_from_db(db)[0])
            except:
                continue
            num_users = len(user2views)
            # print('%d users in %s' % (num_users, db))
            # if num_users != 100:
            #     continue
            for epsilon2views in user2views.values():
                for key, views in epsilon2views.items():
                    ep = float(key[2:])
                    pkg2rhit[pkg][
                        ep] = pkg2rhit[pkg][ep] + views if ep in pkg2rhit[
                            pkg] else views

            pkg2hit[
                pkg] += gorilla.read_actual_hit_num_from_db(db)[0] * num_users

            hist = json.loads(gorilla.read_actual_histogram_from_db(db)[0])
            for n, views in hist.items():
                pkg2name[pkg].add(n)
                if n in pkg2hist[pkg]:
                    pkg2hist[pkg][n] += views * num_users
                else:
                    print('========%sBad%s========' % (Fore.RED, Fore.RESET))
                    sys.exit(-1)

            hist = json.loads(gorilla.read_random_histogram_from_db(db)[0])
            for epsilon2hist in hist.values():
                for key, hist in epsilon2hist.items():
                    ep = float(key[2:])
                    if ep not in pkg2rhist[pkg]:
                        pkg2rhist[pkg][ep] = {}
                        for name in pkg2xname[pkg]:
                            pkg2rhist[pkg][ep][name] = 0
                    for n, views in hist.items():
                        if n in pkg2rhist[pkg][ep]:
                            pkg2rhist[pkg][ep][n] += views
                        else:
                            print('========%sBad%s========' % (Fore.RED,
                                                               Fore.RESET))
                            sys.exit(-1)

    for pkg in pkg2xname:
        xnames = pkg2xname[pkg]
        if not xnames:
            continue
        print('%s ---------------------------' % pkg)
        for ep in sorted(pkg2rhit[pkg]):
            print('\tepsilon=%s' % ep)
            norm(pkg, pkg2hist[pkg], pkg2rhist[pkg][ep], float(ep), plot)
            print('\t\t%sGood%s: %s/%s '
                  '#actual_hits=%s #random_hits=%s' %
                  (Fore.GREEN, Fore.RESET, len(pkg2name[pkg]), len(xnames),
                   pkg2hit[pkg], pkg2rhit[pkg][ep]))

        # print('\t%sPerfect%s: %d-%d \t#hits=%s' % (Fore.YELLOW, Fore.RESET, len(xnames), len(names), num_hits))


if __name__ == '__main__':
    main()
