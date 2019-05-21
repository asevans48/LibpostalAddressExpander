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

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.libpostal.libpostal_normalize_options_t;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.bytedeco.libpostal.global.postal.*;


public class LibpostalExpanderPluginData extends BaseStepData implements StepDataInterface {
  public RowMetaInterface outputRowMeta;

  private AbstractSequenceClassifier classifier;
  private boolean setup1 = false;
  private boolean setup2 = false;
  private boolean isLibPostalInitialized = false;
  private libpostal_normalize_options_t options = null;

  public LibpostalExpanderPluginData() {
    super();
  }

  public AbstractSequenceClassifier getClassifier() {
    return classifier;
  }

  /**
   * Initializse the classifier
   *
   * @param nerPath                     The path to a named entity recognizer
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public void initClassifier(String nerPath) throws IOException, ClassNotFoundException {
    classifier = CRFClassifier.getClassifier(nerPath);
  }

  /**
   * Setup the address parser
   *
   * @param lpPath    The path to the parser
   * @return Whether the system was initialized
   */
  public boolean setupAddressParser(String lpPath){
    if(!setup1 || !setup2) {
      if (lpPath != null && isLibPostalInitialized == false) {
        setup1 = libpostal_setup_datadir(lpPath);
        setup2 = libpostal_setup_language_classifier_datadir(lpPath);
        isLibPostalInitialized = true;
        options = libpostal_get_default_options();
      }
    }
    return isAddressParserSetup();
  }

  /**
   * Whether the address parser is setup
   * @return Whether or not the parser was setup
   */
  public boolean isAddressParserSetup() {
    return (setup1 && setup2);
  }


  /**
   * Parse the address
   *
   * @param text      The text to parse
   * @return  The string array of results
   * @throws UnsupportedEncodingException
   */
  public String[] expandAddress(String text) throws UnsupportedEncodingException {
    BytePointer address = new BytePointer(text, "UTF-8");
    SizeTPointer szptr = new SizeTPointer(0);
    PointerPointer result = libpostal_expand_address(address, options, szptr);
    long t_size = szptr.get();
    String[] results = new String[(int)t_size];
    for(long i = 0; i < t_size; i ++){
      results[(int) i] = result.getString(i);
    }
    result.close();
    return results;
  }

  /**
   * Teardown the libpostal parser
   */
  public void teardownAddressExpander(){
    if(setup1) {
      libpostal_teardown();
    }
    if(setup2) {
      libpostal_teardown_language_classifier();
    }
    setup1 = false;
    setup2 = false;
    isLibPostalInitialized = false;
  }
}
