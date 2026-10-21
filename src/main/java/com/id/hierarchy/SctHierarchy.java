package com.id.hierarchy;

import com.id.sct.SctRelease;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SctHierarchy implements Hierarchy {

  /**
   * concept sct id of root
   */
  private static final long ROOT_SCT_ID = 138875005L;

  /**
   * concepts by internal index, ascending sct id order
   */
  private final Concept[] byIndex;
  /**
   * sorted sct ids
   */
  private final long[] sctIds;
  /**
   * internal index for root concept
   */
  private final int rootIndex;

  /**
   * parent boundaries for concept with internal index i through i + 1
   */
  private final int[] parentOffsets;
  /**
   * flattened child indices
   */
  private final int[] children;
  /**
   * child boundaries for concept with internal index i through i + 1
   */
  private final int[] childOffsets;
  /**
   * ancestor indices for concepts, including self
   */
  private final int[][] ancestorsOrSelf;
  /**
   * depth values as steps for concepts, longest possible path to root
   */
  private final int[] depth;
  /**
   * depth values as doubles for concepts, longest possible path to root
   */
  private final double[] depthScore;
  /**
   * descendant counts, excluding self
   */
  private final int[] subCount;
  /**
   * leaf counts, excluding self
   */
  private final int[] leafSubCount;
  /**
   * total number of concepts without sub concepts
   */
  private final int nbrLeaves;

  public SctHierarchy(SctRelease sctRelease) {
    this.byIndex = sctRelease.concepts().values().stream()
       .sorted(Comparator.comparingLong(Concept::id))
       .toArray(Concept[]::new);

    this.sctIds = extractAndValidateIds(byIndex);
    this.rootIndex = findRootIndex(sctIds);

    int nbrConcepts = byIndex.length;
    long[] edges = encodeEdges(sctRelease.isaRelations());

    Adjacency adjacency = buildAdjacency(nbrConcepts, edges);
    // flattened parent indices
    int[] parents = adjacency.parents();
    this.parentOffsets = adjacency.parentOffsets();
    this.children = adjacency.children();
    this.childOffsets = adjacency.childOffsets();

    validateParentStructure(rootIndex, sctIds, parentOffsets);

    int[] processingOrder = topologicalOrder(nbrConcepts);

    AncestorsAndDepth ancestorData = computeAncestorsAndDepth(
       processingOrder, parents, parentOffsets);

    this.ancestorsOrSelf = ancestorData.ancestorsOrSelf();
    this.depth = ancestorData.depth();
    this.depthScore = Arrays.stream(depth).asDoubleStream().toArray();

    SubConceptCounts counts = countSubConcepts(
       ancestorsOrSelf, childOffsets);

    this.subCount = counts.subConceptCounts();
    this.leafSubCount = counts.leafSubConceptCounts();
    this.nbrLeaves = counts.totalLeafCount();
  }

  /**
   * @return sorted concept sct ids
   */
  private static long[] extractAndValidateIds(Concept[] concepts) {
    long[] ids = new long[concepts.length];
    for (int i = 0; i < concepts.length; i++) {
      ids[i] = concepts[i].id();
      if (i > 0 && ids[i] == ids[i - 1]) {
        throw new IllegalStateException("duplicate concept id " + ids[i]);
      }
    }
    return ids;
  }

  /**
   * @return root index
   */
  private static int findRootIndex(long[] sctIds) {
    int rootIndex = Arrays.binarySearch(sctIds, ROOT_SCT_ID);
    if (rootIndex < 0) {
      throw new IllegalStateException("root concept " + ROOT_SCT_ID + " missing in release");
    }
    return rootIndex;
  }

  /**
   * @return parent and child adjacency construction from edges
   */
  private static Adjacency buildAdjacency(int nbrConcepts, long[] edges) {
    int[] parentOffsets = new int[nbrConcepts + 1];
    int[] childOffsets = new int[nbrConcepts + 1];
    for (long edge : edges) {
      parentOffsets[childOf(edge) + 1]++;
      childOffsets[parentOf(edge) + 1]++;
    }
    for (int i = 0; i < nbrConcepts; i++) {
      parentOffsets[i + 1] += parentOffsets[i];
      childOffsets[i + 1] += childOffsets[i];
    }
    int[] parents = new int[edges.length];
    int[] children = new int[edges.length];
    int[] parentCursors = Arrays.copyOf(parentOffsets, nbrConcepts);
    int[] childCursors = Arrays.copyOf(childOffsets, nbrConcepts);
    for (long edge : edges) {
      int parentIndex = parentOf(edge);
      int childIndex = childOf(edge);
      parents[parentCursors[childIndex]++] = parentIndex;
      children[childCursors[parentIndex]++] = childIndex;
    }
    return new Adjacency(parents, parentOffsets, children, childOffsets);
  }

  /**
   * assert that there is one root and each other concept has at least one parent
   */
  private static void validateParentStructure(int rootIndex, long[] sctIds, int[] parentOffsets) {
    if (parentOffsets[rootIndex] != parentOffsets[rootIndex + 1]) {
      throw new IllegalStateException("root concept has parent concept");
    }
    for (int conceptIndex = 0; conceptIndex < sctIds.length; conceptIndex++) {
      if (conceptIndex != rootIndex
         && parentOffsets[conceptIndex] == parentOffsets[conceptIndex + 1]) {
        throw new IllegalStateException(
           "concept without parent concept " + sctIds[conceptIndex]);
      }
    }
  }

  /**
   * @return ancestor sets and longest root-path lengths
   */
  private static AncestorsAndDepth computeAncestorsAndDepth(
     int[] processingOrder, int[] parents, int[] parentOffsets) {
    int nbrConcepts = processingOrder.length;
    int[][] ancestorsOrSelf = new int[nbrConcepts][];
    int[] depth = new int[nbrConcepts];
    int[] lastSeenStamp = new int[nbrConcepts];
    int[] ancestorBuffer = new int[nbrConcepts];
    for (int conceptIndex : processingOrder) {
      int currentStamp = conceptIndex + 1;
      int ancestorCount = 0;
      int conceptDepth = 0;
      for (int parentPosition = parentOffsets[conceptIndex];
           parentPosition < parentOffsets[conceptIndex + 1];
           parentPosition++) {
        int parentIndex = parents[parentPosition];
        conceptDepth = Math.max(conceptDepth, depth[parentIndex] + 1);
        for (int ancestorIndex : ancestorsOrSelf[parentIndex]) {
          if (lastSeenStamp[ancestorIndex] != currentStamp) {
            lastSeenStamp[ancestorIndex] = currentStamp;
            ancestorBuffer[ancestorCount++] = ancestorIndex;
          }
        }
      }
      ancestorBuffer[ancestorCount++] = conceptIndex;
      int[] conceptAncestorsOrSelf = Arrays.copyOf(ancestorBuffer, ancestorCount);
      Arrays.sort(conceptAncestorsOrSelf);
      ancestorsOrSelf[conceptIndex] = conceptAncestorsOrSelf;
      depth[conceptIndex] = conceptDepth;
    }
    return new AncestorsAndDepth(ancestorsOrSelf, depth);
  }

  /**
   * @return descendant and leaf descendant counts, excluding self
   */
  private static SubConceptCounts countSubConcepts(int[][] ancestorsOrSelf, int[] childOffsets) {
    int nbrConcepts = ancestorsOrSelf.length;
    int[] subConceptCounts = new int[nbrConcepts];
    int[] leafSubConceptCounts = new int[nbrConcepts];
    int totalLeafCount = 0;
    for (int conceptIndex = 0; conceptIndex < nbrConcepts; conceptIndex++) {
      boolean isLeaf =
         childOffsets[conceptIndex] == childOffsets[conceptIndex + 1];

      if (isLeaf) {
        totalLeafCount++;
      }
      for (int ancestorIndex : ancestorsOrSelf[conceptIndex]) {
        if (ancestorIndex == conceptIndex) {
          continue;
        }
        subConceptCounts[ancestorIndex]++;
        if (isLeaf) {
          leafSubConceptCounts[ancestorIndex]++;
        }
      }
    }

    return new SubConceptCounts(
       subConceptCounts,
       leafSubConceptCounts,
       totalLeafCount);
  }

  /**
   * @return sorted edges with parent index in upper 32 bits and child index in lower 32 bits
   */
  private long[] encodeEdges(List<IsaRelation> relations) {
    long[] encoded = new long[relations.size()];
    int r = 0;
    for (IsaRelation relation : relations) {
      int parent = requireIndex(relation.parentConcept().id());
      int child = requireIndex(relation.childConcept().id());
      if (parent == child) {
        throw new IllegalStateException("isa relation to same concept " + sctIds[parent]);
      }
      encoded[r++] = ((long) parent << 32) | child;
    }
    Arrays.sort(encoded);
    int unique = 0;
    for (int i = 0; i < encoded.length; i++) {
      if (i == 0 || encoded[i] != encoded[i - 1]) {
        encoded[unique++] = encoded[i];
      }
    }
    return Arrays.copyOf(encoded, unique);
  }

  /**
   * @return parent index from encoded edge
   */
  private static int parentOf(long edge) {
    return (int) (edge >>> 32);
  }

  /**
   * @return child index from encoded edge
   */
  private static int childOf(long edge) {
    return (int) edge;
  }

  /**
   * @return concept for sct id
   */
  private int requireIndex(long sctId) {
    int index = Arrays.binarySearch(sctIds, sctId);
    if (index < 0) {
      throw new IllegalStateException("isa relation to missing concept " + sctId);
    }
    return index;
  }

  /**
   * Parent-before-child processing order TODO
   */
  private int[] topologicalOrder(int nbrConcepts) {
    int[] inDegree = new int[nbrConcepts];
    for (int c = 0; c < nbrConcepts; c++) {
      inDegree[c] = parentOffsets[c + 1] - parentOffsets[c];
    }
    int[] topo = new int[nbrConcepts];
    int head = 0;
    int tail = 0;
    topo[tail++] = rootIndex;
    while (head < tail) {
      int c = topo[head++];
      for (int i = childOffsets[c]; i < childOffsets[c + 1]; i++) {
        int child = children[i];
        if (--inDegree[child] == 0) {
          topo[tail++] = child;
        }
      }
    }
    if (tail != nbrConcepts) {
      throw new IllegalStateException("cycle in isa relations");
    }
    return topo;
  }

  @Override
  public int numberOfConcepts() {
    return byIndex.length;
  }

  @Override
  public int numberOfLeaves() {
    return nbrLeaves;
  }

  @Override
  public Concept concept(int index) {
    return byIndex[index];
  }

  @Override
  public int indexOfSctId(long sctId) {
    int index = Arrays.binarySearch(sctIds, sctId);
    if (index < 0) {
      throw new IllegalArgumentException("no concept with id " + sctId);
    }
    return index;
  }

  @Override
  public int numberOfSuperConcepts(long sctId) {
    return ancestorsOrSelf[indexOfSctId(sctId)].length - 1;
  }

  @Override
  public int numberOfSubConcepts(long sctId) {
    return subCount[indexOfSctId(sctId)];
  }

  @Override
  public int numberOfLeafSubConcepts(long sctId) {
    return leafSubCount[indexOfSctId(sctId)];
  }

  @Override
  public boolean isSubConceptOf(long sub, long sup) {
    int subIndex = indexOfSctId(sub);
    int supIndex = indexOfSctId(sup);
    return subIndex != supIndex && Arrays.binarySearch(ancestorsOrSelf[subIndex], supIndex) >= 0;
  }

  @Override
  public int depth(long sctId) {
    return depth[indexOfSctId(sctId)];
  }

  @Override
  public long lca(long a, long b) {
    return commonAncestorMaximizing(a, b, depthScore);
  }

  @Override
  public long commonAncestorMaximizing(long a, long b, double[] score) {
    if (score.length != byIndex.length) {
      throw new IllegalArgumentException("score array must have length " + byIndex.length + " but has length " + score.length);
    }
    int[] x = ancestorsOrSelf[indexOfSctId(a)];
    int[] y = ancestorsOrSelf[indexOfSctId(b)];
    int i = 0;
    int j = 0;
    int best = -1;
    while (i < x.length && j < y.length) {
      if (x[i] < y[j]) {
        i++;
      } else if (x[i] > y[j]) {
        j++;
      } else {
        int c = x[i];
        if (best == -1 || score[c] > score[best]) {
          best = c;
        }
        i++;
        j++;
      }
    }
    return sctIds[best];
  }

  private record Adjacency(
     int[] parents,
     int[] parentOffsets,
     int[] children,
     int[] childOffsets) {
  }

  private record AncestorsAndDepth(
     int[][] ancestorsOrSelf,
     int[] depth) {
  }

  private record SubConceptCounts(
     int[] subConceptCounts,
     int[] leafSubConceptCounts,
     int totalLeafCount) {
  }
}