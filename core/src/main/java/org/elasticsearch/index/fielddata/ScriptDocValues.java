/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.fielddata;


import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.GeoUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;


/**
 * Script level doc values, the assumption is that any implementation will implement a <code>getValue</code>
 * and a <code>getValues</code> that return the relevant type that then can be used in scripts.
 */
public interface ScriptDocValues<T> extends List<T> {

    /**
     * Set the current doc ID.
     */
    void setNextDocId(int docId);

    /**
     * Return a copy of the list of the values for the current document.
     */
    List<T> getValues();

    public static final class Strings extends AbstractList<String> implements ScriptDocValues<String> {

        private final SortedBinaryDocValues values;

        public Strings(SortedBinaryDocValues values) {
            this.values = values;
        }

        @Override
        public void setNextDocId(int docId) {
            values.setDocument(docId);
        }

        public SortedBinaryDocValues getInternalValues() {
            return this.values;
        }

        public BytesRef getBytesValue() {
            if (values.count() > 0) {
                return values.valueAt(0);
            } else {
                return null;
            }
        }

        public String getValue() {
            BytesRef value = getBytesValue();
            if (value == null) {
                return null;
            } else {
                return value.utf8ToString();
            }
        }

        @Override
        public List<String> getValues() {
            return Collections.unmodifiableList(this);
        }

        @Override
        public String get(int index) {
            return values.valueAt(index).utf8ToString();
        }

        @Override
        public int size() {
            return values.count();
        }

    }

    public static class Longs extends AbstractList<Long> implements ScriptDocValues<Long> {

        private final SortedNumericDocValues values;
        private final MutableDateTime date = new MutableDateTime(0, DateTimeZone.UTC);

        public Longs(SortedNumericDocValues values) {
            this.values = values;
        }

        @Override
        public void setNextDocId(int docId) {
            values.setDocument(docId);
        }

        public SortedNumericDocValues getInternalValues() {
            return this.values;
        }

        public long getValue() {
            int numValues = values.count();
            if (numValues == 0) {
                return 0L;
            }
            return values.valueAt(0);
        }

        @Override
        public List<Long> getValues() {
            return Collections.unmodifiableList(this);
        }

        public ReadableDateTime getDate() {
            date.setMillis(getValue());
            return date;
        }

        @Override
        public Long get(int index) {
            return values.valueAt(index);
        }

        @Override
        public int size() {
            return values.count();
        }

    }

    public static class Doubles extends AbstractList<Double> implements ScriptDocValues<Double> {

        private final SortedNumericDoubleValues values;

        public Doubles(SortedNumericDoubleValues values) {
            this.values = values;
        }

        @Override
        public void setNextDocId(int docId) {
            values.setDocument(docId);
        }

        public SortedNumericDoubleValues getInternalValues() {
            return this.values;
        }

        public double getValue() {
            int numValues = values.count();
            if (numValues == 0) {
                return 0d;
            }
            return values.valueAt(0);
        }

        @Override
        public List<Double> getValues() {
            return Collections.unmodifiableList(this);
        }

        @Override
        public Double get(int index) {
            return values.valueAt(index);
        }

        @Override
        public int size() {
            return values.count();
        }
    }

    class GeoPoints extends AbstractList<GeoPoint> implements ScriptDocValues<GeoPoint> {

        private final MultiGeoPointValues values;

        public GeoPoints(MultiGeoPointValues values) {
            this.values = values;
        }

        @Override
        public void setNextDocId(int docId) {
            values.setDocument(docId);
        }

        public GeoPoint getValue() {
            int numValues = values.count();
            if (numValues == 0) {
                return null;
            }
            return values.valueAt(0);
        }

        public double getLat() {
            return getValue().lat();
        }

        public double[] getLats() {
            List<GeoPoint> points = getValues();
            double[] lats = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                lats[i] = points.get(i).lat();
            }
            return lats;
        }

        public double[] getLons() {
            List<GeoPoint> points = getValues();
            double[] lons = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                lons[i] = points.get(i).lon();
            }
            return lons;
        }

        public double getLon() {
            return getValue().lon();
        }

        @Override
        public List<GeoPoint> getValues() {
            return Collections.unmodifiableList(this);
        }

        @Override
        public GeoPoint get(int index) {
            final GeoPoint point = values.valueAt(index);
            return new GeoPoint(point.lat(), point.lon());
        }

        @Override
        public int size() {
            return values.count();
        }

        public double arcDistance(double lat, double lon) {
            GeoPoint point = getValue();
            return GeoUtils.arcDistance(point.lat(), point.lon(), lat, lon);
        }

        public double arcDistanceWithDefault(double lat, double lon, double defaultValue) {
            if (isEmpty()) {
                return defaultValue;
            }
            return arcDistance(lat, lon);
        }

        public double planeDistance(double lat, double lon) {
            GeoPoint point = getValue();
            return GeoUtils.planeDistance(point.lat(), point.lon(), lat, lon);
        }

        public double planeDistanceWithDefault(double lat, double lon, double defaultValue) {
            if (isEmpty()) {
                return defaultValue;
            }
            return planeDistance(lat, lon);
        }

        public double geohashDistance(String geohash) {
            GeoPoint point = getValue();
            return GeoUtils.arcDistance(point.lat(), point.lon(), GeoHashUtils.decodeLatitude(geohash),
                GeoHashUtils.decodeLongitude(geohash));
        }

        public double geohashDistanceWithDefault(String geohash, double defaultValue) {
            if (isEmpty()) {
                return defaultValue;
            }
            return geohashDistance(geohash);
        }
    }

    final class Booleans extends AbstractList<Boolean> implements ScriptDocValues<Boolean> {

        private final SortedNumericDocValues values;

        public Booleans(SortedNumericDocValues values) {
            this.values = values;
        }

        @Override
        public void setNextDocId(int docId) {
            values.setDocument(docId);
        }

        @Override
        public List<Boolean> getValues() {
            return this;
        }

        public boolean getValue() {
            return values.count() != 0 && values.valueAt(0) == 1;
        }

        @Override
        public Boolean get(int index) {
            return values.valueAt(index) == 1;
        }

        @Override
        public int size() {
            return values.count();
        }

    }

    public static class BytesRefs extends AbstractList<BytesRef> implements ScriptDocValues<BytesRef> {

        private final SortedBinaryDocValues values;

        public BytesRefs(SortedBinaryDocValues values) {
            this.values = values;
        }

        @Override
        public void setNextDocId(int docId) {
            values.setDocument(docId);
        }

        public SortedBinaryDocValues getInternalValues() {
            return this.values;
        }

        public BytesRef getValue() {
            int numValues = values.count();
            if (numValues == 0) {
                return new BytesRef();
            }
            return values.valueAt(0);
        }

        @Override
        public List<BytesRef> getValues() {
            return Collections.unmodifiableList(this);
        }

        @Override
        public BytesRef get(int index) {
            return values.valueAt(index);
        }

        @Override
        public int size() {
            return values.count();
        }
    }

}
