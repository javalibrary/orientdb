package com.orientechnologies.orient.core.storage.index.sbtree.local;

import com.orientechnologies.common.directmemory.OByteBufferPool;
import com.orientechnologies.common.directmemory.OPointer;
import com.orientechnologies.common.serialization.types.OLongSerializer;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.orientechnologies.orient.core.storage.cache.OCacheEntry;
import com.orientechnologies.orient.core.storage.cache.OCachePointer;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * @author Andrey Lomakin (a.lomakin-at-orientdb.com)
 * @since 12.08.13
 */
public class SBTreeNonLeafBucketTest {
  @Test
  public void testInitialization() {
    final OByteBufferPool bufferPool = OByteBufferPool.instance(null);
    final OPointer pointer = bufferPool.acquireDirect(true);

    OCachePointer cachePointer = new OCachePointer(pointer, bufferPool, 0, 0);
    OCacheEntry cacheEntry = new OCacheEntry(0, 0, cachePointer);
    cacheEntry.acquireExclusiveLock();
    cachePointer.incrementReferrer();

    OSBTreeBucket<Long, OIdentifiable> treeBucket = new OSBTreeBucket<>(cacheEntry, false);
    Assert.assertEquals(treeBucket.size(), 0);
    Assert.assertFalse(treeBucket.isLeaf());

    treeBucket = new OSBTreeBucket<>(cacheEntry);
    Assert.assertEquals(treeBucket.size(), 0);
    Assert.assertFalse(treeBucket.isLeaf());
    Assert.assertEquals(treeBucket.getLeftSibling(), -1);
    Assert.assertEquals(treeBucket.getRightSibling(), -1);

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

  @Test
  public void testSearch() {
    long seed = System.currentTimeMillis();
    System.out.println("testSearch seed : " + seed);

    TreeSet<Long> keys = new TreeSet<>();
    Random random = new Random(seed);

    while (keys.size() < 2 * OSBTreeBucket.MAX_PAGE_SIZE_BYTES / OLongSerializer.LONG_SIZE) {
      keys.add(random.nextLong());
    }

    final OByteBufferPool bufferPool = OByteBufferPool.instance(null);
    final OPointer pointer = bufferPool.acquireDirect(true);

    OCachePointer cachePointer = new OCachePointer(pointer, bufferPool, 0, 0);
    OCacheEntry cacheEntry = new OCacheEntry(0, 0, cachePointer);
    cacheEntry.acquireExclusiveLock();
    cachePointer.incrementReferrer();

    OSBTreeBucket<Long, OIdentifiable> treeBucket = new OSBTreeBucket<>(cacheEntry, false);

    int index = 0;
    Map<Long, Integer> keyIndexMap = new HashMap<>();
    for (Long key : keys) {
      if (!treeBucket.insertEntry(index,
          new OSBTreeBucket.SBTreeEntry<>(random.nextInt(Integer.MAX_VALUE), random.nextInt(Integer.MAX_VALUE), key, null),
          OLongSerializer.INSTANCE, null, null, OLinkSerializer.INSTANCE))
        break;

      keyIndexMap.put(key, index);
      index++;
    }

    Assert.assertEquals(treeBucket.size(), keyIndexMap.size());

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      int bucketIndex = treeBucket.find(keyIndexEntry.getKey(), null, OLongSerializer.INSTANCE);
      Assert.assertEquals(bucketIndex, (int) keyIndexEntry.getValue());
    }

    long prevRight = -1;
    for (int i = 0; i < treeBucket.size(); i++) {
      OSBTreeBucket.SBTreeEntry<Long, OIdentifiable> entry = treeBucket
          .getEntry(i, null, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      if (prevRight > 0)
        Assert.assertEquals(entry.leftChild, prevRight);

      prevRight = entry.rightChild;
    }

    long prevLeft = -1;
    for (int i = treeBucket.size() - 1; i >= 0; i--) {
      OSBTreeBucket.SBTreeEntry<Long, OIdentifiable> entry = treeBucket
          .getEntry(i, null, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      if (prevLeft > 0)
        Assert.assertEquals(entry.rightChild, prevLeft);

      prevLeft = entry.leftChild;
    }

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

  @Test
  public void testShrink() {
    long seed = System.currentTimeMillis();
    System.out.println("testShrink seed : " + seed);

    TreeSet<Long> keys = new TreeSet<>();
    Random random = new Random(seed);

    while (keys.size() < 2 * OSBTreeBucket.MAX_PAGE_SIZE_BYTES / OLongSerializer.LONG_SIZE) {
      keys.add(random.nextLong());
    }

    final OByteBufferPool bufferPool = OByteBufferPool.instance(null);
    final OPointer pointer = bufferPool.acquireDirect(true);

    OCachePointer cachePointer = new OCachePointer(pointer, bufferPool, 0, 0);
    OCacheEntry cacheEntry = new OCacheEntry(0, 0, cachePointer);
    cacheEntry.acquireExclusiveLock();

    cachePointer.incrementReferrer();

    OSBTreeBucket<Long, OIdentifiable> treeBucket = new OSBTreeBucket<>(cacheEntry, false);

    int index = 0;
    for (Long key : keys) {
      if (!treeBucket
          .insertEntry(index, new OSBTreeBucket.SBTreeEntry<>(index, index + 1, key, null), OLongSerializer.INSTANCE, null, null,
              OLinkSerializer.INSTANCE))
        break;

      index++;
    }

    int originalSize = treeBucket.size();

    treeBucket.shrink(treeBucket.size() / 2, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE, false);
    Assert.assertEquals(treeBucket.size(), index / 2);

    index = 0;
    final Map<Long, Integer> keyIndexMap = new HashMap<>();

    Iterator<Long> keysIterator = keys.iterator();
    while (keysIterator.hasNext() && index < treeBucket.size()) {
      Long key = keysIterator.next();
      keyIndexMap.put(key, index);
      index++;
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      int bucketIndex = treeBucket.find(keyIndexEntry.getKey(), null, OLongSerializer.INSTANCE);
      Assert.assertEquals(bucketIndex, (int) keyIndexEntry.getValue());
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      OSBTreeBucket.SBTreeEntry<Long, OIdentifiable> entry = treeBucket
          .getEntry(keyIndexEntry.getValue(), null, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      Assert.assertEquals(entry,
          new OSBTreeBucket.SBTreeEntry<Long, OIdentifiable>(keyIndexEntry.getValue(), keyIndexEntry.getValue() + 1,
              keyIndexEntry.getKey(), null));
    }

    int keysToAdd = originalSize - treeBucket.size();
    int addedKeys = 0;
    while (keysIterator.hasNext() && index < originalSize) {
      Long key = keysIterator.next();

      if (!treeBucket
          .insertEntry(index, new OSBTreeBucket.SBTreeEntry<>(index, index + 1, key, null), OLongSerializer.INSTANCE, null, null,
              OLinkSerializer.INSTANCE))
        break;

      keyIndexMap.put(key, index);
      index++;
      addedKeys++;
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      OSBTreeBucket.SBTreeEntry<Long, OIdentifiable> entry = treeBucket
          .getEntry(keyIndexEntry.getValue(), null, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      Assert.assertEquals(entry,
          new OSBTreeBucket.SBTreeEntry<Long, OIdentifiable>(keyIndexEntry.getValue(), keyIndexEntry.getValue() + 1,
              keyIndexEntry.getKey(), null));
    }

    Assert.assertEquals(treeBucket.size(), originalSize);
    Assert.assertEquals(addedKeys, keysToAdd);

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

}
