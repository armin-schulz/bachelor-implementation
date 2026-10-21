"""
plot 6: shares of the is-a categories per soft config as stacked bars, one figure per variant, per config a bar for
the mapping concepts (precision side) and one for the reference concepts (recall side), the share of concepts whose
category depends on tie breaking above each bar

usage: python plot_categories.py <csv directory of Evaluation> <plot directory>, plots are written to categories
"""
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.patches import Patch

from plot_common import (CATEGORIES_PLOT_DIR, CONFIG_LABELS, SOFT_CONFIGS, TEXT_WIDTH_INCH, VARIANT_LABELS, VARIANTS,
                         format_decimal, save)

SIDES = {"p": "P", "r": "R"}
CATEGORIES = {
    "eq": r"$\mathrm{K}_{=}$ entspricht",
    "gt": r"$\mathrm{K}_{>}$ allgemeiner",
    "lt": r"$\mathrm{K}_{<}$ spezieller",
    "neq": r"$\mathrm{K}_{-}$ nicht vergleichbar",
}
CATEGORY_COLORS = {"eq": "#1b9e77", "gt": "#d95f02", "lt": "#7570b3", "neq": "#bdbdbd"}
COLUMNS = ["variant", "config"] + [f"{side} {column}" for side in SIDES
                                   for column in ["concepts"] + list(CATEGORIES) + ["ambiguous"]]
BAR_WIDTH = 0.36
SHARE_TOLERANCE = 1e-9


def read_categories(csv_dir):
    """
    @return shares indexed by variant and config
    @raise ValueError if columns are unexpected, a variant or config is missing or the shares of a side do not sum
    to 1
    """
    path = Path(csv_dir) / "category_shares.csv"
    df = pd.read_csv(path, sep="\t", float_precision="round_trip")
    if list(df.columns) != COLUMNS:
        raise ValueError(f"unexpected columns in {path}: {list(df.columns)}")
    df = df.set_index(["variant", "config"])
    expected = pd.MultiIndex.from_product([VARIANTS, SOFT_CONFIGS])
    if set(df.index) != set(expected) or len(df) != len(expected):
        raise ValueError(f"expected {list(expected)} in {path}, got {list(df.index)}")
    for side in SIDES:
        sums = df[[f"{side} {category}" for category in CATEGORIES]].sum(axis=1)
        if ((sums - 1).abs() > SHARE_TOLERANCE).any():
            raise ValueError(f"shares of side {side} do not sum to 1 in {path}")
    return df


def plot(shares, variant):
    rows = shares.loc[variant].loc[SOFT_CONFIGS]
    x = np.arange(len(SOFT_CONFIGS))
    fig, ax = plt.subplots(figsize=(TEXT_WIDTH_INCH, 3.2))
    for s, (side, side_label) in enumerate(SIDES.items()):
        positions = x + (s - (len(SIDES) - 1) / 2) * BAR_WIDTH * 1.1
        bottom = np.zeros(len(SOFT_CONFIGS))
        for category in CATEGORIES:
            values = rows[f"{side} {category}"].to_numpy()
            ax.bar(positions, values, bottom=bottom, width=BAR_WIDTH, color=CATEGORY_COLORS[category],
                   edgecolor="black", linewidth=0.5)
            bottom += values
        for position, ambiguous in zip(positions, rows[f"{side} ambiguous"]):
            ax.text(position, 1.01, f"{format_decimal(100 * ambiguous, 1)} %", ha="center", va="bottom",
                    fontsize=plt.rcParams["legend.fontsize"] - 1.5)
            ax.text(position, -0.02, side_label, transform=ax.get_xaxis_transform(), ha="center", va="top",
                    fontsize=plt.rcParams["xtick.labelsize"])
    ax.set_xticks(x)
    ax.set_xticklabels([CONFIG_LABELS[config].replace(" (", "\n(") for config in SOFT_CONFIGS])
    ax.tick_params(axis="x", length=0, pad=14)
    ax.set_xlim(-0.6, len(SOFT_CONFIGS) - 0.4)
    ax.set_ylim(0, 1.1)
    ax.set_yticks(np.linspace(0, 1, 6))
    ax.set_ylabel("Anteil der Konzepte")
    ax.grid(axis="x", visible=False)
    ax.set_title(VARIANT_LABELS[variant])
    fig.tight_layout()
    handles = [Patch(facecolor=CATEGORY_COLORS[category], edgecolor="black", label=label)
               for category, label in CATEGORIES.items()]
    fig.legend(handles=handles, loc="upper center", bbox_to_anchor=(0.5, 0), ncol=len(CATEGORIES), frameon=False)
    fig.text(0.5, -0.09, "P: Mappingkonzepte (Precision), R: Referenzkonzepte (Recall), "
                         "über den Balken: Anteil mehrdeutiger Zuordnungen",
             ha="center", va="top", fontsize=plt.rcParams["legend.fontsize"] - 1)
    return fig


def main(csv_dir, plot_dir):
    shares = read_categories(csv_dir)
    for variant in VARIANTS:
        save(plot(shares, variant), plot_dir, CATEGORIES_PLOT_DIR, variant)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
