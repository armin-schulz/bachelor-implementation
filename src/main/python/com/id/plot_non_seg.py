"""
plot 3: boxplots of f of the main config per text, texts without against texts with non segmentable parts, grouped by
variant, from the prepared statistics of NonSegEvaluation, whiskers within 1.5 interquartile ranges, no outliers

usage: python plot_non_seg.py <csv directory of NonSegEvaluation> <plot directory>, plot is written to nonseg
"""
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.patches import Patch

from plot_common import (CONFIG_LABELS, FIGURE_SIZE, MAIN_CONFIG, NON_SEG_PLOT_DIR, SCORE_LABELS, VARIANTS,
                         format_count, format_decimal, save)

COLUMNS = ["variant", "non seg", "count", "mean", "std", "min", "lower whisker", "q1", "median", "q3",
           "upper whisker", "max"]
GROUPS = {False: "ohne nicht segmentierbare Teile", True: "mit nicht segmentierbaren Teilen"}
COLORS = {False: "tab:blue", True: "tab:orange"}
BOX_WIDTH = 0.6
GROUP_GAP = 1.0


def read_non_seg(csv_dir):
    """
    @return statistics indexed by variant and non seg
    @raise ValueError if columns are unexpected or a group is missing
    """
    path = Path(csv_dir) / "non_seg.csv"
    df = pd.read_csv(path, sep="\t", true_values=["true"], false_values=["false"], float_precision="round_trip")
    if list(df.columns) != COLUMNS:
        raise ValueError(f"unexpected columns in {path}: {list(df.columns)}")
    df = df.set_index(["variant", "non seg"])
    expected = pd.MultiIndex.from_product([VARIANTS, list(GROUPS)])
    if not df.index.equals(expected):
        raise ValueError(f"expected groups {list(expected)} in {path}, got {list(df.index)}")
    return df


def box_stats(row):
    return {"med": row["median"], "q1": row["q1"], "q3": row["q3"], "whislo": row["lower whisker"],
            "whishi": row["upper whisker"], "mean": row["mean"], "fliers": []}


def plot(csv_dir):
    stats = read_non_seg(csv_dir)
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    for v, variant in enumerate(VARIANTS):
        for g, non_seg in enumerate(GROUPS):
            row = stats.loc[(variant, non_seg)]
            position = v * (len(GROUPS) + GROUP_GAP) + g
            ax.bxp([box_stats(row)], positions=[position], widths=BOX_WIDTH, showmeans=True, patch_artist=True,
                   boxprops={"facecolor": COLORS[non_seg], "alpha": 0.4},
                   medianprops={"color": "black"},
                   meanprops={"marker": "D", "markerfacecolor": "white", "markeredgecolor": "black",
                              "markersize": 4})
            ax.text(position, -0.04, f"n = {format_count(row['count'])}\n"
                                     f"x̄ = {format_decimal(row['mean'])}\nx̃ = {format_decimal(row['median'])}",
                    transform=ax.get_xaxis_transform(), ha="center", va="top",
                    fontsize=plt.rcParams["xtick.labelsize"])
    ax.set_xticks([v * (len(GROUPS) + GROUP_GAP) + (len(GROUPS) - 1) / 2 for v in range(len(VARIANTS))])
    ax.set_xticklabels([f"Variante {variant}" for variant in VARIANTS])
    ax.tick_params(axis="x", length=0, pad=42)
    ax.set_ylim(0, 1.02)
    ax.set_ylabel(f"{SCORE_LABELS['f']}, {CONFIG_LABELS[MAIN_CONFIG]}")
    ax.grid(axis="x", visible=False)
    fig.tight_layout()
    handles = [Patch(facecolor=COLORS[non_seg], alpha=0.4, edgecolor="black", label=label)
               for non_seg, label in GROUPS.items()]
    fig.legend(handles=handles, loc="upper center", bbox_to_anchor=(0.5, 0), ncol=len(GROUPS), frameon=False)
    return fig


def main(csv_dir, plot_dir):
    save(plot(csv_dir), plot_dir, NON_SEG_PLOT_DIR, "non_seg")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
