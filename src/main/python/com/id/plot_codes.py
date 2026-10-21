"""
plot 4: distribution of the code values of variant B, exact against main config, a boxplot of precision, recall and f
from the prepared statistics of Evaluation and one histogram per score from the code values

usage: python plot_codes.py <csv directory of Evaluation> <plot directory>, plots are written to codes
"""
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.patches import Patch

from plot_common import (CODES_PLOT_DIR, CONFIG_COLORS, CONFIG_LABELS, EXACT, FIGURE_SIZE, MAIN_CONFIG, SCORE_LABELS,
                         format_count, format_decimal, save)

CONFIGS = [EXACT, MAIN_CONFIG]
CODE_COLUMNS = ["config", "code", "chapter", "texts", "precision", "recall", "f"]
DISTRIBUTION_COLUMNS = ["config", "score", "count", "mean", "std", "min", "lower whisker", "q1", "median", "q3",
                        "upper whisker", "max"]
BOX_WIDTH = 0.35
BINS = np.linspace(0, 1, 21)


def read_codes(csv_dir):
    """
    @return code values of the configs
    @raise ValueError if columns or configs are unexpected or the configs differ in their codes
    """
    path = Path(csv_dir) / "codes.csv"
    df = pd.read_csv(path, sep="\t", dtype={"code": str}, float_precision="round_trip")
    if list(df.columns) != CODE_COLUMNS:
        raise ValueError(f"unexpected columns in {path}: {list(df.columns)}")
    if set(df["config"]) != set(CONFIGS):
        raise ValueError(f"expected configs {CONFIGS} in {path}, got {sorted(set(df['config']))}")
    codes = [set(df.loc[df["config"] == config, "code"]) for config in CONFIGS]
    if any(c != codes[0] for c in codes):
        raise ValueError(f"configs differ in their codes in {path}")
    return df


def read_distribution(csv_dir):
    """
    @return statistics of the code values indexed by config and score
    @raise ValueError if columns are unexpected or a config or score is missing
    """
    path = Path(csv_dir) / "code_distribution.csv"
    df = pd.read_csv(path, sep="\t", float_precision="round_trip")
    if list(df.columns) != DISTRIBUTION_COLUMNS:
        raise ValueError(f"unexpected columns in {path}: {list(df.columns)}")
    df = df.set_index(["config", "score"])
    expected = pd.MultiIndex.from_product([CONFIGS, list(SCORE_LABELS)])
    if set(df.index) != set(expected) or len(df) != len(expected):
        raise ValueError(f"expected {list(expected)} in {path}, got {list(df.index)}")
    return df


def config_label(config):
    return CONFIG_LABELS[config] if config == EXACT else f"graduell, {CONFIG_LABELS[config]}"


def legend_below(fig):
    handles = [Patch(facecolor=CONFIG_COLORS[config], alpha=0.4, edgecolor="black", label=config_label(config))
               for config in CONFIGS]
    fig.legend(handles=handles, loc="upper center", bbox_to_anchor=(0.5, 0), ncol=len(CONFIGS), frameon=False)


def plot_boxplot(csv_dir):
    stats = read_distribution(csv_dir)
    counts = set(stats["count"])
    if len(counts) != 1:
        raise ValueError(f"different numbers of codes {counts}")
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    for s, score in enumerate(SCORE_LABELS):
        for c, config in enumerate(CONFIGS):
            row = stats.loc[(config, score)]
            position = s + (c - (len(CONFIGS) - 1) / 2) * BOX_WIDTH * 1.2
            ax.bxp([{"med": row["median"], "q1": row["q1"], "q3": row["q3"], "whislo": row["lower whisker"],
                     "whishi": row["upper whisker"], "mean": row["mean"], "fliers": []}],
                   positions=[position], widths=BOX_WIDTH, showmeans=True, patch_artist=True,
                   boxprops={"facecolor": CONFIG_COLORS[config], "alpha": 0.4},
                   medianprops={"color": "black"},
                   meanprops={"marker": "D", "markerfacecolor": "white", "markeredgecolor": "black",
                              "markersize": 4})
            ax.text(position, -0.04, f"x̄ = {format_decimal(row['mean'])}\nx̃ = {format_decimal(row['median'])}",
                    transform=ax.get_xaxis_transform(), ha="center", va="top",
                    fontsize=plt.rcParams["legend.fontsize"] - 1)
    ax.set_xticks(range(len(SCORE_LABELS)))
    ax.set_xticklabels(SCORE_LABELS.values())
    ax.tick_params(axis="x", length=0, pad=28)
    ax.set_xlim(-0.6, len(SCORE_LABELS) - 0.4)
    ax.set_ylim(0, 1.02)
    ax.set_ylabel("Mittelwert je Code")
    ax.grid(axis="x", visible=False)
    ax.text(0.98, 0.03, f"n = {format_count(counts.pop())} Codes", transform=ax.transAxes, ha="right", va="bottom",
            fontsize=plt.rcParams["legend.fontsize"],
            bbox={"facecolor": "white", "edgecolor": "none", "alpha": 0.8, "pad": 1.5})
    fig.tight_layout()
    legend_below(fig)
    return fig


def plot_histogram(codes, score):
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    for config in CONFIGS:
        values = codes.loc[codes["config"] == config, score]
        ax.hist(values, bins=BINS, histtype="stepfilled", color=CONFIG_COLORS[config], alpha=0.4, edgecolor="black",
                linewidth=0.6)
    ax.set_xlim(0, 1)
    ax.set_xlabel(f"{SCORE_LABELS[score]}, Mittelwert je Code")
    ax.set_ylabel("Anzahl Codes")
    ax.yaxis.get_major_locator().set_params(integer=True)
    ax.set_title(SCORE_LABELS[score])
    fig.tight_layout()
    legend_below(fig)
    return fig


def main(csv_dir, plot_dir):
    save(plot_boxplot(csv_dir), plot_dir, CODES_PLOT_DIR, "boxplot")
    codes = read_codes(csv_dir)
    for score in SCORE_LABELS:
        save(plot_histogram(codes, score), plot_dir, CODES_PLOT_DIR, f"histogram_{score}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
