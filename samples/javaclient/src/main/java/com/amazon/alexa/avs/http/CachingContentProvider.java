/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs.http;

import org.eclipse.jetty.client.api.ContentProvider;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Decorates a {@link ContentProvider} and adds caching behavior to allow for HTTP request retries.
 */
public class CachingContentProvider implements ContentProvider.Typed {
    private ContentProvider contentProvider;
    private CachingIterator cachingIterator;

    public CachingContentProvider(ContentProvider contentProvider) {
        this.contentProvider = contentProvider;
    }

    @Override
    public long getLength() {
        return contentProvider.getLength();
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
        if (cachingIterator == null) {
            cachingIterator = new CachingIterator(contentProvider.iterator());
            return cachingIterator;
        } else {
            return cachingIterator.cache.iterator();
        }
    }

    @Override
    public String getContentType() {
        if (contentProvider instanceof ContentProvider.Typed) {
            return ((ContentProvider.Typed) contentProvider).getContentType();
        }
        return null;
    }

    /**
     * Keeps a cache of ByteBuffers that come from the original iterator.
     */
    public static class CachingIterator implements Iterator<ByteBuffer> {
        private Iterator<ByteBuffer> originalIterator;
        private List<ByteBuffer> cache = new LinkedList<ByteBuffer>();

        public CachingIterator(Iterator<ByteBuffer> originalIterator) {
            this.originalIterator = originalIterator;
        }

        @Override
        public boolean hasNext() {
            return originalIterator.hasNext();
        }

        @Override
        public ByteBuffer next() {
            ByteBuffer byteBuffer = originalIterator.next();
            cache.add(byteBuffer.duplicate());
            return byteBuffer;
        }
    }
}
