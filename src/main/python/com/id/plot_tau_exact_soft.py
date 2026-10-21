"""
plot 1: tau curves of the main measure, exact against soft, one figure per variant and score,
area under exact (gray) is the mean of exact, area between exact and soft (blue) the mean gain of soft,
y axis from 0 to 1 with psi on the left and the number of units on the right

usage: python plot_tau_exact_soft.py <csv directory of Evaluation> <plot directory>, plots are written to tau
"""
import sys

import matplotlib.pyplot as plt

from plot_common import (CONFIG_COLORS, CONFIG_LABELS, EXACT, FIGURE_SIZE, MAIN_CONFIG, SCORE_LABELS, TAU_PLOT_DIR,
                         VARIANTS, fill_between_tau, fill_under_tau, plot_tau, read_tau, save, style_tau_axis)


def plot(csv_dir, variant, score):
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    exact = read_tau(csv_dir, variant, EXACT, score)
    soft = read_tau(csv_dir, variant, MAIN_CONFIG, score)
    fill_under_tau(ax, exact, color=CONFIG_COLORS[EXACT], alpha=0.15)
    fill_between_tau(ax, exact, soft, color=CONFIG_COLORS[MAIN_CONFIG], alpha=0.15)
    plot_tau(ax, exact, color=CONFIG_COLORS[EXACT], label=CONFIG_LABELS[EXACT])
    plot_tau(ax, soft, color=CONFIG_COLORS[MAIN_CONFIG], label=f"graduell, {CONFIG_LABELS[MAIN_CONFIG]}")
    ax.set_title(SCORE_LABELS[score])
    style_tau_axis(ax, [exact, soft], variant)
    fig.tight_layout()
    fig.legend(loc="upper center", bbox_to_anchor=(0.5, 0), ncol=2, frameon=False)
    return fig


def main(csv_dir, plot_dir):
    for variant in VARIANTS:
        for score in SCORE_LABELS:
            save(plot(csv_dir, variant, score), plot_dir, TAU_PLOT_DIR, f"exact_soft_{variant}_{score}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
