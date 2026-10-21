from pathlib import Path

import matplotlib
from pandas.plotting import plot_params

matplotlib.use("pdf")

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.ticker import FuncFormatter

EXACT = "exact"
MAIN_CONFIG = "Lin-Sanchez"
SOFT_CONFIGS = [MAIN_CONFIG, "Lin-Seco", "JC-Sanchez", "JC-Seco", "WP"]
VARIANTS = ["A", "B"]

CONFIG_LABELS = {
    EXACT: "exakt",
    "Lin-Sanchez": "Lin (Sánchez)",
    "Lin-Seco": "Lin (Seco)",
    "JC-Sanchez": "Jiang-Conrath (Sánchez)",
    "JC-Seco": "Jiang-Conrath (Seco)",
    "WP": "Wu-Palmer",
}
CONFIG_COLORS = {
    EXACT: "tab:gray",
    "Lin-Sanchez": "tab:blue",
    "Lin-Seco": "tab:orange",
    "JC-Sanchez": "tab:green",
    "JC-Seco": "tab:red",
    "WP": "tab:purple",
}
SCORE_LABELS = {"precision": "Precision", "recall": "Recall", "f": "F-Maß"}
UNIT_LABELS = {"A": "Anzahl Vorschläge", "B": "Anzahl Texte"}
VARIANT_LABELS = {"A": "Variante A: alle Vorschläge", "B": "Variante B: best matching proposal"}

TAU_PLOT_DIR = "tau"
NON_SEG_PLOT_DIR = "nonseg"
CODES_PLOT_DIR = "codes"
CHAPTERS_PLOT_DIR = "chapters"
CATEGORIES_PLOT_DIR = "categories"

TEXT_WIDTH_INCH = 6.0
FIGURE_SIZE = (0.75 * TEXT_WIDTH_INCH, 3.0)
PSI_AT_ONE_TOLERANCE = 0.001

plt.rcParams.update({
    "font.size": 9,
    "axes.titlesize": 9,
    "axes.labelsize": 9,
    "legend.fontsize": 8,
    "xtick.labelsize": 8,
    "ytick.labelsize": 8,
    "axes.grid": True,
    "grid.alpha": 0.3,
    "pdf.fonttype": 42,
})


def read_tau(csv_dir, variant, config, score):
    """
    @return steps (tau, psi, count) of a tau curve, psi and count hold on (previous tau, tau]
    @raise ValueError if the curve is malformed
    """
    path = Path(csv_dir) / "tau" / f"{variant}_{config}_{score}.csv"
    curve = pd.read_csv(path, sep="\t", float_precision="round_trip")
    if (list(curve.columns) != ["tau", "psi", "count"] or curve.empty or curve["tau"].iloc[0] != 0
            or curve["tau"].iloc[-1] != 1 or curve["psi"].iloc[0] != 1
            or not curve["tau"].is_monotonic_increasing or not curve["count"].is_monotonic_decreasing
            or (curve["psi"] != curve["count"] / curve["count"].iloc[0]).any()):
        raise ValueError(f"malformed tau curve {path}")
    return curve


def units(*curves):
    """
    @return number of evaluation units of curves
    @raise ValueError if curves differ in their number of units
    """
    counts = {int(curve["count"].iloc[0]) for curve in curves}
    if len(counts) != 1:
        raise ValueError(f"tau curves with different numbers of units {counts}")
    return counts.pop()


def count_at_one(*curves):
    """
    @return smallest number of units with value 1 of curves, in theory equal for all curves of a score, since a
    similarity is 1 only for identical concepts
    @raise ValueError if psi(1) of curves differs by more than PSI_AT_ONE_TOLERANCE, e.g. due to rounding of
    similarities of different concepts to 1
    """
    counts = [int(curve["count"].iloc[-1]) for curve in curves]
    n = units(*curves)
    if (max(counts) - min(counts)) / n > PSI_AT_ONE_TOLERANCE:
        raise ValueError(f"psi(1) of tau curves differs by more than {PSI_AT_ONE_TOLERANCE}: {counts} of {n}")
    return min(counts)


def plot_tau(ax, curve, **kwargs):
    """
    draws the step function exactly, where="pre" holds psi of a point on the interval left of it, not clipped
    and above the axis lines so that a step at psi(1) stays visible on the x axis
    """
    kwargs.setdefault("zorder", 3)
    ax.step(curve["tau"], curve["psi"], where="pre", clip_on=False, **kwargs)


def on_steps(curve, taus):
    """
    @param taus sorted values in [0, 1]
    @return psi of curve at taus, i.e. psi of the first step with tau >= t
    """
    return curve["psi"].to_numpy()[np.searchsorted(curve["tau"].to_numpy(), taus, side="left")]


def fill_under_tau(ax, curve, **kwargs):
    """
    fills the area under a tau curve down to 0 exactly, its size is the mean of the values
    """
    ax.fill_between(curve["tau"], 0, curve["psi"], step="pre", linewidth=0, **kwargs)


def fill_between_tau(ax, lower, upper, **kwargs):
    """
    fills the area between two tau curves exactly, using the union of their steps
    """
    taus = np.union1d(lower["tau"].to_numpy(), upper["tau"].to_numpy())
    ax.fill_between(taus, on_steps(lower, taus), on_steps(upper, taus), step="pre", linewidth=0, **kwargs)


def style_tau_axis(ax, curves, variant, left_label=True, right_label=True, annotate=True):
    """
    y axis from 0 to 1 with psi on the left and the absolute number of units on the right

    @param curves   tau curves drawn in ax
    @param annotate whether to write psi(1) with its number of units in the upper right corner, smallest of curves
    @raise ValueError if annotate and psi(1) of curves differs, see count_at_one
    """
    n = units(*curves)
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1.02)
    ax.set_xlabel(r"$\tau$")
    if left_label:
        ax.set_ylabel(r"$\Psi(\tau)$")
    right = ax.secondary_yaxis("right", functions=(lambda psi: psi * n, lambda c: c / n))
    right.yaxis.set_major_formatter(FuncFormatter(lambda c, _: format_count(c)))
    if right_label:
        right.set_ylabel(UNIT_LABELS[variant])
    if annotate:
        count = count_at_one(*curves)
        ax.text(0.97, 0.95, rf"$\Psi(1)$ = {format_decimal(count / n)}" + f"\n({format_count(count)})",
                transform=ax.transAxes, ha="right", va="top", ma="right", fontsize=plt.rcParams["legend.fontsize"],
                bbox={"facecolor": "white", "edgecolor": "none", "alpha": 0.8, "pad": 1.5}, zorder=4)


def format_decimal(value, digits=2):
    return f"{value:.{digits}f}".replace(".", ",")


def format_count(count):
    return f"{count:,.0f}".replace(",", ".")


def save(fig, plot_dir, subdir, name):
    """
    writes fig to plot_dir/subdir/name.pdf
    """
    plot_subdir = Path(plot_dir) / subdir
    plot_subdir.mkdir(parents=True, exist_ok=True)
    path = plot_subdir / f"{name}.pdf"
    fig.savefig(path, bbox_inches="tight")
    plt.close(fig)
    print(f"written {path}")
