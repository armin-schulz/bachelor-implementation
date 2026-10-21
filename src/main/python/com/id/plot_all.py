"""
creates all plots from the csv directory of Evaluation and NonSegEvaluation, stops at the first failing plot

usage: python plot_all.py <csv directory> <plot directory>
"""
import sys

import plot_categories
import plot_chapters
import plot_codes
import plot_non_seg
import plot_tau_configs
import plot_tau_exact_soft

PLOTS = [plot_tau_exact_soft, plot_tau_configs, plot_non_seg, plot_codes, plot_chapters, plot_categories]


def main(csv_dir, plot_dir):
    for plot in PLOTS:
        plot.main(csv_dir, plot_dir)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
