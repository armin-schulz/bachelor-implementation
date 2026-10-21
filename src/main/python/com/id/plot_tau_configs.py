"""
plot 2: tau curves of f of all soft configs and of exact f, one figure per variant,
y axis from 0 to 1 with psi on the left and the number of units on the right

usage: python plot_tau_configs.py <csv directory of Evaluation> <plot directory>, plots are written to tau
"""
import sys

import matplotlib.pyplot as plt

from plot_common import (CONFIG_COLORS, CONFIG_LABELS, EXACT, FIGURE_SIZE, MAIN_CONFIG, SCORE_LABELS, SOFT_CONFIGS,
                         TAU_PLOT_DIR, VARIANTS, plot_tau, read_tau, save, style_tau_axis)

SCORE = "f"


def plot(csv_dir, variant):
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    exact = read_tau(csv_dir, variant, EXACT, SCORE)
    plot_tau(ax, exact, color=CONFIG_COLORS[EXACT], linewidth=1.0, label=CONFIG_LABELS[EXACT])
    curves = [exact]
    for config in SOFT_CONFIGS:
        curve = read_tau(csv_dir, variant, config, SCORE)
        main = config == MAIN_CONFIG
        plot_tau(ax, curve, color=CONFIG_COLORS[config], linewidth=1.8 if main else 1.0, zorder=3.5 if main else 3,
                 label=CONFIG_LABELS[config])
        curves.append(curve)
    ax.set_title(SCORE_LABELS[SCORE])
    style_tau_axis(ax, curves, variant)
    fig.tight_layout()
    fig.legend(loc="upper center", bbox_to_anchor=(0.5, 0), ncol=3, frameon=False)
    return fig


def main(csv_dir, plot_dir):
    for variant in VARIANTS:
        save(plot(csv_dir, variant), plot_dir, TAU_PLOT_DIR, f"configs_{variant}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
