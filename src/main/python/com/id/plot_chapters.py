"""
plot 5: chapter values of variant B as grouped bars, exact against main config, one figure per score, chapters in
order of the chapter file, the number of codes per chapter behind the chapter

usage: python plot_chapters.py <csv directory of Evaluation> <plot directory>, plots are written to chapters
"""
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.patches import Patch

from plot_common import (CHAPTERS_PLOT_DIR, CONFIG_COLORS, CONFIG_LABELS, EXACT, MAIN_CONFIG, SCORE_LABELS,
                         TEXT_WIDTH_INCH, format_count, save)

CONFIGS = [EXACT, MAIN_CONFIG]
COLUMNS = ["config", "chapter", "codes", "texts", "precision", "recall", "f"]
BAR_WIDTH = 0.4


def read_chapters(csv_dir):
    """
    @return chapter values indexed by config and chapter, chapters in order of the file
    @raise ValueError if columns or configs are unexpected or the configs differ in chapters, codes or texts
    """
    path = Path(csv_dir) / "chapters.csv"
    df = pd.read_csv(path, sep="\t", dtype={"chapter": str}, float_precision="round_trip")
    if list(df.columns) != COLUMNS:
        raise ValueError(f"unexpected columns in {path}: {list(df.columns)}")
    if set(df["config"]) != set(CONFIGS):
        raise ValueError(f"expected configs {CONFIGS} in {path}, got {sorted(set(df['config']))}")
    counts = [df.loc[df["config"] == config, ["chapter", "codes", "texts"]].reset_index(drop=True)
              for config in CONFIGS]
    if any(not c.equals(counts[0]) for c in counts):
        raise ValueError(f"configs differ in chapters, codes or texts in {path}")
    return df.set_index(["config", "chapter"])


def plot(chapters, score):
    names = list(chapters.loc[EXACT].index)
    x = np.arange(len(names))
    fig, ax = plt.subplots(figsize=(TEXT_WIDTH_INCH, 3.0))
    for c, config in enumerate(CONFIGS):
        values = chapters.loc[config].loc[names, score]
        ax.bar(x + (c - (len(CONFIGS) - 1) / 2) * BAR_WIDTH, values, width=BAR_WIDTH,
               color=CONFIG_COLORS[config], alpha=0.6, edgecolor="black", linewidth=0.5)
    codes = chapters.loc[EXACT].loc[names, "codes"]
    ax.set_xticks(x)
    ax.set_xticklabels([f"{name} ({format_count(count)})" for name, count in zip(names, codes)], rotation=90)
    ax.tick_params(axis="x", length=0)
    ax.set_xlim(-0.6, len(names) - 0.4)
    ax.set_ylim(0, 1.02)
    ax.set_xlabel("ICD-10-GM-Kapitel (Anzahl Codes)")
    ax.set_ylabel(f"{SCORE_LABELS[score]}, Mittel der Codewerte")
    ax.grid(axis="x", visible=False)
    ax.set_title(SCORE_LABELS[score])
    fig.tight_layout()
    handles = [Patch(facecolor=CONFIG_COLORS[config], alpha=0.6, edgecolor="black",
                     label=CONFIG_LABELS[config] if config == EXACT else f"graduell, {CONFIG_LABELS[config]}")
               for config in CONFIGS]
    fig.legend(handles=handles, loc="upper center", bbox_to_anchor=(0.5, 0), ncol=len(CONFIGS), frameon=False)
    return fig


def main(csv_dir, plot_dir):
    chapters = read_chapters(csv_dir)
    for score in SCORE_LABELS:
        save(plot(chapters, score), plot_dir, CHAPTERS_PLOT_DIR, score)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
