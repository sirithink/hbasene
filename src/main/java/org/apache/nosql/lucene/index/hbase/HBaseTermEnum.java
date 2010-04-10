/**
 * Copyright 2010 Karthik Kumar
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nosql.lucene.index.hbase;

import java.io.IOException;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

/**
 * Implementation of Term Enumerator. <br />
 * 
 * 
 * 
 */
public class HBaseTermEnum extends TermEnum {

  private final HTable table;

  private ResultScanner resultScanner;

  private Term currentTerm;

  public HBaseTermEnum(final Configuration conf, final String indexName)
      throws IOException {
    // TODO: Check out some scanner caching options.
    table = new HTable(conf, indexName);
    this.resultScanner = table
        .getScanner(HBaseIndexTransactionLog.FAMILY_TERMVECTOR);
  }

  @Override
  public void close() throws IOException {
    this.resultScanner.close();
    this.table.close();
  }

  @Override
  public int docFreq() {
    try {
      Get get = new Get(Bytes.toBytes(this.currentTerm.field() + "/"
          + this.currentTerm.text()));
      get.addFamily(HBaseIndexTransactionLog.FAMILY_TERMVECTOR);
      Result result = this.table.get(get);
      if (result == null) {
        return 0;
      }
      NavigableMap<byte[], byte[]> map = result
          .getFamilyMap(HBaseIndexTransactionLog.FAMILY_TERMVECTOR);
      return map.size();
    } catch (Exception ex) {
      return 0;
    }
  }

  @Override
  public boolean next() {
    try {
      Result result = resultScanner.next();
      if (result != null) {
        String fieldTerm = Bytes.toString(result.getRow());
        String[] fieldTerms = fieldTerm.split(",");
        this.currentTerm = new Term(fieldTerms[0], fieldTerms[1]);
        return true;
      } else {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }
  }

  @Override
  public Term term() {
    return this.currentTerm;
  }

  /**
   * Directly skip to a given term.
   * 
   * @param t 
   * @throws IOException
   */
  public void skipTo(Term t) throws IOException {
    if (this.resultScanner != null) {
      this.resultScanner.close();
    }
    Scan scan = new Scan();
    scan.addFamily(HBaseIndexTransactionLog.FAMILY_TERMVECTOR);
    scan.setStartRow(Bytes.toBytes(t.field() + "/" + t.text()));
    this.resultScanner = this.table.getScanner(scan);
  }

}
