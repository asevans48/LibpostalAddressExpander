/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.si;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Describe your step plugin.
 * 
 */
public class LibpostalExpanderPlugin extends BaseStep implements StepInterface {

  private LibpostalExpanderPluginMeta meta;
  private LibpostalExpanderPluginData data;

  private static Class<?> PKG = LibpostalExpanderPluginMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$
  
  public LibpostalExpanderPlugin( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
    Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }
  class OnExitHook extends Thread{

    @Override
    public void run(){
      data.teardownAddressExpander();
    }
  }

  private void initNER(){
    try {
      if(data.getClassifier() == null) {
        if(meta.getNerPath() == null){
          throw new NullPointerException("No NLP Model Specified");
        }

        data.initClassifier(meta.getNerPath());
        if(data.getClassifier() == null){
          if(isBasic()){
            logBasic("NER Classifier Not Loaded");
          }
          stopAll();
        }else{
          if(isBasic()){
            logBasic("NER Classifier Loaded");
          }
        }
      }
    }catch(IOException e){
      if(isBasic()){
        logBasic("Failed to Load Core NLP Classifier");
      }
    }catch(NullPointerException e) {
      if(isBasic()){
        logBasic("Path to Model Was Null");
      }
    }catch(ClassNotFoundException e){
      if(isBasic()){
        logBasic("Failed to find Class when Loading Core NLP Model");
      }
    }
  }

  private void initAddressParser(){
    boolean wasSetup = data.setupAddressParser(meta.getLpPath());
    if(!wasSetup){
      if(isBasic()){
        logBasic("Failed to Initialize Address Parser");
      }
      stopAll();
    }else{
      if(isBasic()){
        logBasic("Address Parser Loaded");
      }
    }
  }

  /**
   * Initialize and do work where other steps need to wait for...
   *
   * @param stepMetaInterface
   *          The metadata to work with
   * @param stepDataInterface
   *          The data to initialize
   */
  @Override
  public boolean init(StepMetaInterface stepMetaInterface, StepDataInterface stepDataInterface ) {
    this.meta = (LibpostalExpanderPluginMeta) stepMetaInterface;
    this.data = (LibpostalExpanderPluginData) stepDataInterface;
    initNER();
    initAddressParser();
    Runtime.getRuntime().addShutdownHook(new OnExitHook());
    return super.init( stepMetaInterface, stepDataInterface );
  }

  /**
   * Checks whether a string contains a specific location.
   *
   * @param text  The text to check against
   * @return  Whether the text contains the location
   */
  private boolean stringContainsLocation(String text){
    boolean contains_loc = false;
    if(text != null) {
      if(data.getClassifier() != null) {
        List<List<CoreLabel>> labels = data.getClassifier().classify(text);
        for (List<CoreLabel> sentence : labels) {
          for (CoreLabel word : sentence) {
            String ctype = word.get(CoreAnnotations.AnswerAnnotation.class);
            if (ctype.toUpperCase().equals("LOCATION")) {
              contains_loc = true;
            }
          }
        }
      }else{
        throw new NullPointerException("Classifier For Location Detection Null");
      }
    }
    return contains_loc;
  }


  private Object[] packageRow(RowMetaInterface rmi, String result, Object[] r){
    int idx = rmi.indexOfValue(meta.getExpandField());
    r[idx] = result;
    return r;
  }


  /**
   * Parse the address from the appropriate field
   *
   * @param rmi       The RowMetaInterface containing output meta
   * @param process   Whether to process the row
   * @param text      The text to process
   * @param r         The row to process
   * @return  The object array row representation
   */
  private Object[][] expandAddress(RowMetaInterface rmi, boolean process, String text, Object[] r){
    Object[][] orows = new Object[1][rmi.size()];
    orows[0] = r;
    if(process && text != null) {
      try {
        String[] results = data.expandAddress(text);
        if(results.length > 0){
          int max_iter = meta.isAddAll() ? results.length: 1;
          orows = new Object[results.length][rmi.size()];
          if(max_iter > 1) {
            for (int i = 0; i < max_iter; i++) {
              String res = results[i];
              Object[] r2 = packageRow(rmi, res, r.clone());
              orows[i] = r2;
            }
          }else if(max_iter == 1){
            String res = results[0];
            Object[] r2 = packageRow(rmi,res, r.clone());
            orows[0] = r2;
          }
        }
      }catch(UnsupportedEncodingException e){
        if(isBasic()){
          logBasic("Failed to expand address");
          logBasic(e.getMessage());
          e.printStackTrace();
        }
      }
    }
    return orows;
  }

  /**
   * Get and set the address fields.
   *
   * @param rowMeta  The row meta interface
   * @param r        The existing row object without field values;
   * @return  The new row with values
   */
  private Object[][] computeRowValues(RowMetaInterface rowMeta, Object[] r){
    Object[] orow = r.clone();

    if(rowMeta.size() > r.length){
      orow = RowDataUtil.resizeArray(r, rowMeta.size());
    }

    Object[][] orows = new Object[1][rowMeta.size()];
    orows[0] = r;

    if(meta.getExpandIndex() >= 0) {
      int idx = rowMeta.indexOfValue(meta.getExpandInField());
      String extractText = (String) r[idx];
      boolean process = true;
      if(meta.isNer() && !stringContainsLocation(extractText)){
        process = false;
      }
      if(process){
        orows = expandAddress(data.outputRowMeta, process, extractText, orow);
      }
    }

    return orows;
  }

  /**
   * Check if the value exists in the array
   *
   * @param arr  The array to check
   * @param v    The value in the array
   * @return  Whether the value exists
   */
  private int stringArrayContains(String[] arr, String v){
    int exists = -1;
    int i = 0;
    while(i < arr.length && exists == -1){
      if(arr[i].equals(v)){
        exists = i;
      }else {
        i += 1;
      }
    }
    return exists;
  }

  /**
   * Recreate the meta, adding the new fields.
   *
   * @param rowMeta   The row meta
   * @return  The changed row meta interface
   */
  private RowMetaInterface getNewRowMeta(RowMetaInterface rowMeta, LibpostalExpanderPluginMeta meta) throws KettleException {
    String[] fields = rowMeta.getFieldNames();
    String[] fieldnames = {};

    int idx = stringArrayContains(fields, meta.getExpandField());
    if(idx == -1){
      throw new KettleException("Libpostal Plugin missing Extract Field");
    }
    meta.setExpandIndex(idx);

    for(int i = 0; i < fieldnames.length; i++){
      String fname = fieldnames[i];
      int cidx = stringArrayContains(fields, fname);
      if(cidx == -1){
        ValueMetaInterface value = ValueMetaFactory.createValueMeta(fname, ValueMetaInterface.TYPE_STRING);
        rowMeta.addValueMeta(value);
      }
    }
    return rowMeta;
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    Object[] r = getRow();
    if ( r == null ) {
      setOutputDone();
      data.teardownAddressExpander();
      return false;
    }

    if(first) {
      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields(data.outputRowMeta, getStepname(), null, null, this, null, null);
      getNewRowMeta(data.outputRowMeta, meta);
      data.teardownAddressExpander();
      first = false;
    }

    Object[][] outRows = computeRowValues(data.outputRowMeta, r);
    if(outRows.length > 0) {
      for (Object[] outRow : outRows) {
        putRow(data.outputRowMeta, outRow); // copy row to possible alternate rowset(s).
      }
    }else{
      putRow(data.outputRowMeta, r); //should not reach, just in case
    }

    if ( checkFeedback( getLinesRead() ) ) {
      if ( log.isBasic() )
        logBasic( BaseMessages.getString( PKG, "LibpostalExpanderPlugin.Log.LineNumber" ) + getLinesRead() );
    }

    return true;
  }
}