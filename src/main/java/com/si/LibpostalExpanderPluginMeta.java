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

import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;


/**
 * Skeleton for PDI Step plugin.
 */
@Step( id = "LibpostalExpanderPlugin", image = "LibpostalExpanderPlugin.svg", name = "Normalize Address",
    description = "Expands and normalizes addresses.", categoryDescription = "Transform" )
public class LibpostalExpanderPluginMeta extends BaseStepMeta implements StepMetaInterface {
  
  private static Class<?> PKG = LibpostalExpanderPlugin.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  public LibpostalExpanderPluginMeta() {
    super(); // allocate BaseStepMeta
  }

  private boolean isNer;
  private boolean isAddAll;
  private String nerPath;
  private String lpPath;
  private String expandInField;
  private int expandIndex = -1;
  private String expandField = null;

  public String getExpandInField() {
    return expandInField;
  }

  public void setExpandInField(String expandInField) {
    this.expandInField = expandInField;
  }

  public boolean isAddAll() {
    return isAddAll;
  }

  public void setAddAll(boolean addAll) {
    isAddAll = addAll;
  }

  public String getExpandField() {
    return expandField;
  }

  public void setExpandField(String expandField) {
    this.expandField = expandField;
  }

  public boolean isNer() {
    return isNer;
  }

  public void setNer(boolean ner) {
    isNer = ner;
  }

  public int getExpandIndex() {
    return expandIndex;
  }

  public void setExpandIndex(int expandIndex) {
    this.expandIndex = expandIndex;
  }

  public String getNerPath() {
    return nerPath;
  }

  public void setNerPath(String nerPath) {
    this.nerPath = nerPath;
  }

  public String getLpPath() {
    return lpPath;
  }

  public void setLpPath(String lpPath) {
    this.lpPath = lpPath;
  }

  public String getXML() throws KettleValueException {
    StringBuilder xml = new StringBuilder();
    xml.append( XMLHandler.addTagValue( "expandField", expandField ) );
    xml.append(XMLHandler.addTagValue("expandIndex", expandIndex));
    xml.append(XMLHandler.addTagValue("nerPath", nerPath));
    xml.append(XMLHandler.addTagValue("lpPath", lpPath));
    xml.append(XMLHandler.addTagValue("expandInField", expandInField));
    xml.append(XMLHandler.addTagValue("isNer", isNer));
    xml.append(XMLHandler.addTagValue("isAddAll", isAddAll));
    return xml.toString();
  }

  public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
    try {
      setExpandField(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "expandField")));
      setExpandIndex(Integer.valueOf(Const.NVL(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "expandIndex")), "-1")));
      setNerPath(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "nerPath")));
      setLpPath(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "lpPath")));
      setExpandInField(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "expandInField")));
      setNer(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "isNer")).toUpperCase().equals("Y"));
      setAddAll(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "isAddAll")).toUpperCase().equals("Y"));
    } catch ( Exception e ) {
      throw new KettleXMLException( "Demo plugin unable to read step info from XML node", e );
    }
  }

  public Object clone() {
    Object retval = super.clone();
    return retval;
  }

  public void setDefault() {
    isNer = false;
    isAddAll = false;
    nerPath = "";
    lpPath = "";
    expandInField = "";
    expandIndex = -1;
    expandField = "";
  }

  public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
    try {
      expandField  = rep.getStepAttributeString(id_step, "expandField" );
      expandIndex = (int) rep.getStepAttributeInteger(id_step, "expandIndex");
      nerPath = rep.getStepAttributeString(id_step, "nerPath");
      lpPath = rep.getStepAttributeString(id_step, "lpPath");
      expandInField = rep.getStepAttributeString(id_step, "expandInField");
      isAddAll = rep.getStepAttributeBoolean(id_step, "isAddAll");
      isNer = rep.getStepAttributeBoolean(id_step, "isNer");
    } catch ( Exception e ) {
      throw new KettleException( "Unable to load step from repository", e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "expandField", expandField);
      rep.saveStepAttribute( id_transformation, id_step, "expandIndex", expandIndex);
      rep.saveStepAttribute( id_transformation, id_step, "nerPath", nerPath);
      rep.saveStepAttribute( id_transformation, id_step, "lpPath", lpPath);
      rep.saveStepAttribute( id_transformation, id_step, "expandInField", expandInField);
      rep.saveStepAttribute( id_transformation, id_step, "isNer", isNer);
      rep.saveStepAttribute( id_transformation, id_step, "isAddAll", isAddAll);
    } catch ( Exception e ) {
      throw new KettleException( "Unable to save step into repository: " + id_step, e );
    }
  }

  public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep,
                        VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {
      ValueMetaInterface v0 = new ValueMetaString(expandField);
      v0.setTrimType(ValueMetaInterface.TRIM_TYPE_BOTH);
      v0.setOrigin(name);
      inputRowMeta.addValueMeta(v0);
  }

  public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
                    String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository,
                    IMetaStore metaStore ) {
    CheckResult cr;
    if ( prev == null || prev.size() == 0 ) {
      cr =
              new CheckResult( CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString( PKG,
                      "LibpostalExpanderPluginMeta.CheckResult.NotReceivingFields" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
              new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString( PKG,
                      "LibpostalExpanderPluginMeta.CheckResult.StepRecevingData", prev.size() + "" ), stepMeta );
      remarks.add( cr );
    }

    // See if we have input streams leading to this step!
    if ( input.length > 0 ) {
      cr =
              new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString( PKG,
                      "LibpostalExpanderPluginMeta.CheckResult.StepRecevingData2" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
              new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
                      "LibpostalExpanderPluginMeta.CheckResult.NoInputReceivedFromOtherSteps" ), stepMeta );
      remarks.add( cr );
    }
  }

  public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr, Trans trans) {
    return new LibpostalExpanderPlugin(stepMeta, stepDataInterface, cnr, tr, trans);
  }

  public StepDataInterface getStepData() {
    return new LibpostalExpanderPluginData();
  }

}
