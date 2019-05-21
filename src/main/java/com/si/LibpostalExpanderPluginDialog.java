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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class LibpostalExpanderPluginDialog extends BaseStepDialog implements StepDialogInterface {

  private static Class<?> PKG = LibpostalExpanderPluginMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  private LibpostalExpanderPluginMeta meta;

  private Label wlStepname;
  private Text wStepname;
  private FormData fdStepname, fdlStepname;

  private Label lfname;
  private CCombo expandCombo;
  private FormData fdlFname, fdStep;

  private Label wExpandName;
  private TextVar wExpandOut;
  private FormData fdlExpandName, fdlExpandOut;

  private Label wLpName;
  private TextVar wLpOut;
  private FormData fdlLpName, fdlLpOut;

  private Label wNerName;
  private TextVar wNerOut;
  private FormData fdlNerName, fdlNerOut;

  private Label wUseNerName;
  private Button wuseNer;
  private FormData fdlUseNer, fdlUseNerName;

  private Label wAddAllName;
  private Button wAddAll;
  private FormData fdlAddAll, fdlAddAllName;

  public LibpostalExpanderPluginDialog(Shell parent, Object stepMeta, TransMeta transMeta, String stepname ) {
    super( parent, (BaseStepMeta) stepMeta, transMeta, stepname );
    meta = (LibpostalExpanderPluginMeta) stepMeta;
  }

  @Override
  public String open() {
    // store some convenient SWT variables
    Shell parent = getParent();
    Display display = parent.getDisplay();

    // SWT code for preparing the dialog
    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    props.setLook(shell);
    setShellImage(shell, meta);

    // Save the value of the changed flag on the meta object. If the user cancels
    // the dialog, it will be restored to this saved value.
    // The "changed" variable is inherited from BaseStepDialog
    changed = meta.hasChanged();

    // The ModifyListener used on all controls. It will update the meta object to
    // indicate that changes are being made.
    ModifyListener lsMod = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        meta.setChanged();
      }
    };

    // ------------------------------------------------------- //
    // SWT code for building the actual settings dialog        //
    // ------------------------------------------------------- //
    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;
    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "LibpostalExpanderPluginDialog.Shell.Title"));
    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    wlStepname = new Label(shell, SWT.RIGHT);
    wlStepname.setText(BaseMessages.getString(PKG, "LibpostalExpanderPluginDialog.Stepname.Label"));
    props.setLook(wlStepname);
    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment(0, 0);
    fdlStepname.right = new FormAttachment(middle, -margin);
    fdlStepname.top = new FormAttachment(0, margin);
    wlStepname.setLayoutData(fdlStepname);

    wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wStepname.setText(stepname);
    props.setLook(wStepname);
    wStepname.addModifyListener(lsMod);
    fdStepname = new FormData();
    fdStepname.left = new FormAttachment(middle, 0);
    fdStepname.top = new FormAttachment(0, margin);
    fdStepname.right = new FormAttachment(100, 0);
    wStepname.setLayoutData(fdStepname);

    // Set the field name
    lfname = new Label( shell, SWT.RIGHT );
    lfname.setText( BaseMessages.getString( PKG, "LibpostalExpanderPluginDialog.Fields.FieldName" ) );
    props.setLook( lfname );
    fdlFname = new FormData();
    fdlFname.left = new FormAttachment( 0, 0 );
    fdlFname.right = new FormAttachment( middle, -margin );
    fdlFname.top = new FormAttachment( wStepname, 15 );
    lfname.setLayoutData( fdlFname );

    expandCombo = new CCombo( shell, SWT.BORDER );
    props.setLook( expandCombo );
    StepMeta stepinfo = transMeta.findStep( stepname );
    if ( stepinfo != null ) {
      try {
        String[] fields = transMeta.getStepFields(stepname).getFieldNames();
        for (int i = 0; i < fields.length; i++) {
          expandCombo.add(fields[i]);
        }
      }catch(KettleException e){
        if ( log.isBasic())
          logBasic("Failed to Get Step Fields");
      }
    }

    expandCombo.addModifyListener( lsMod );
    fdStep = new FormData();
    fdStep.left = new FormAttachment( middle, 0 );
    fdStep.top = new FormAttachment( wStepname, 15 );
    fdStep.right = new FormAttachment( 100, 0 );
    expandCombo.setLayoutData( fdStep );

    // the expansion address out field
    wExpandName = new Label(shell, SWT.RIGHT);
    wExpandName.setText(BaseMessages.getString(PKG, "LibpostalExpanderPluginDialog.Output.Expansion"));
    props.setLook(wExpandName);
    fdlExpandName = new FormData();
    fdlExpandName.left = new FormAttachment(0, 0);
    fdlExpandName.top = new FormAttachment(lfname, 15);
    fdlExpandName.right = new FormAttachment(middle, -margin);
    wExpandName.setLayoutData(fdlExpandName);
    wExpandOut = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wExpandOut.setText("");
    wExpandOut.addModifyListener(lsMod);
    props.setLook(wExpandOut);
    fdlExpandOut = new FormData();
    fdlExpandOut.left = new FormAttachment(middle, 0);
    fdlExpandOut.top = new FormAttachment(lfname, 15);
    fdlExpandOut.right = new FormAttachment(100, 0);
    wExpandOut.setLayoutData(fdlExpandOut);

    //path to NER
    wNerName = new Label(shell, SWT.RIGHT);
    wNerName.setText(BaseMessages.getString(PKG, "LibpostalExpanderPluginDialog.Output.NERModel"));
    props.setLook(wNerName);
    fdlNerName = new FormData();
    fdlNerName.left = new FormAttachment(0, 0);
    fdlNerName.top = new FormAttachment(wExpandName, 15);
    fdlNerName.right = new FormAttachment(middle, -margin);
    wNerName.setLayoutData(fdlNerName);
    wNerOut = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wNerOut.setText("");
    wNerOut.addModifyListener(lsMod);
    props.setLook(wNerOut);
    fdlNerOut = new FormData();
    fdlNerOut.left = new FormAttachment(middle, 0);
    fdlNerOut.top = new FormAttachment(wExpandName, 15);
    fdlNerOut.right = new FormAttachment(100, 0);
    wNerOut.setLayoutData(fdlNerOut);


    //path to libpostal
    wLpName = new Label(shell, SWT.RIGHT);
    wLpName.setText(BaseMessages.getString(PKG, "LibpostalExpanderPluginDialog.Output.LpPath"));
    props.setLook(wLpName);
    fdlLpName = new FormData();
    fdlLpName.left = new FormAttachment(0, 0);
    fdlLpName.top = new FormAttachment(wNerName, 15);
    fdlLpName.right = new FormAttachment(middle, -margin);
    wLpName.setLayoutData(fdlLpName);
    wLpOut = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wLpOut.setText("");
    wLpOut.addModifyListener(lsMod);
    props.setLook(wLpOut);
    fdlLpOut = new FormData();
    fdlLpOut.left = new FormAttachment(middle, 0);
    fdlLpOut.top = new FormAttachment(wNerName, 15);
    fdlLpOut.right = new FormAttachment(100, 0);
    wLpOut.setLayoutData(fdlLpOut);

    //add a flag for using ner
    wUseNerName = new Label(shell,SWT.RIGHT);
    wUseNerName.setText(BaseMessages.getString(PKG,"LibpostalExpanderPluginDialog.Output.Usener"));
    props.setLook(wUseNerName);
    fdlUseNerName = new FormData();
    fdlUseNerName.left = new FormAttachment(0, 0);
    fdlUseNerName.top = new FormAttachment(wLpName, 15);
    fdlUseNerName.right = new FormAttachment(middle, -margin);
    wUseNerName.setLayoutData(fdlUseNerName);
    wuseNer = new Button(shell, SWT.CHECK);
    props.setLook(wuseNer);
    fdlUseNer = new FormData();
    fdlUseNer.left = new FormAttachment(middle, 0);
    fdlUseNer.top = new FormAttachment(wLpName, 15);
    fdlUseNer.right = new FormAttachment(100, 0);
    wuseNer.setLayoutData(fdlUseNer);

    //add a flag for keeping all fields
    wAddAllName = new Label(shell,SWT.RIGHT);
    wAddAllName.setText(BaseMessages.getString(PKG,"LibpostalExpanderPluginDialog.Output.KeepAll"));
    props.setLook(wAddAllName);
    fdlAddAllName = new FormData();
    fdlAddAllName.left = new FormAttachment(0, 0);
    fdlAddAllName.top = new FormAttachment(wUseNerName, 15);
    fdlAddAllName.right = new FormAttachment(middle, -margin);
    wAddAllName.setLayoutData(fdlAddAllName);
    wAddAll = new Button(shell, SWT.CHECK);
    props.setLook(wAddAll);
    fdlAddAll = new FormData();
    fdlAddAll.left = new FormAttachment(middle, 0);
    fdlAddAll.top = new FormAttachment(wUseNerName, 15);
    fdlAddAll.right = new FormAttachment(100, 0);
    wAddAll.setLayoutData(fdlAddAll);

    // OK and cancel buttons
    wOK = new Button(shell, SWT.PUSH);
    wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    setButtonPositions(new Button[]{wOK, wCancel}, margin, wAddAllName);

    // Add listeners for cancel and OK
    lsCancel = new Listener() {
      public void handleEvent(Event e) {
        cancel();
      }
    };
    lsOK = new Listener() {
      public void handleEvent(Event e) {
        ok();
      }
    };
    wCancel.addListener(SWT.Selection, lsCancel);
    wOK.addListener(SWT.Selection, lsOK);

    // default listener (for hitting "enter")
    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected(SelectionEvent e) {
        ok();
      }
    };
    wStepname.addSelectionListener(lsDef);
    wExpandOut.addSelectionListener(lsDef);
    wuseNer.addSelectionListener(lsDef);
    wAddAll.addSelectionListener(lsDef);
    wNerOut.addSelectionListener(lsDef);
    wLpOut.addSelectionListener(lsDef);


    // Detect X or ALT-F4 or something that kills this window and cancel the dialog properly
    shell.addShellListener(new ShellAdapter() {
      public void shellClosed(ShellEvent e) {
        cancel();
      }
    });

    // Set/Restore the dialog size based on last position on screen
    // The setSize() method is inherited from BaseStepDialog
    setSize();

    // populate the dialog with the values from the meta object
    getData();

    // restore the changed flag to original value, as the modify listeners fire during dialog population
    meta.setChanged(changed);

    // open dialog and enter event loop
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }

    // at this point the dialog has closed, so either ok() or cancel() have been executed
    // The "stepname" variable is inherited from BaseStepDialog
    return stepname;
  }

  /**
   * Copy information from the meta-data input to the dialog fields.
   */
  public void getData() {
    wStepname.selectAll();
    expandCombo.setText(Const.NVL(meta.getExpandInField(), ""));
    wExpandOut.setText(Const.NVL(meta.getExpandField(), ""));
    wuseNer.setSelection(meta.isNer());
    wAddAll.setSelection(meta.isAddAll());
    wLpOut.setText(Const.NVL(meta.getLpPath(), ""));
    wNerOut.setText(Const.NVL(meta.getNerPath(), ""));
    wStepname.setFocus();
  }

  private Image getImage() {
    PluginInterface plugin =
        PluginRegistry.getInstance().getPlugin( StepPluginType.class, stepMeta.getStepMetaInterface() );
    String id = plugin.getIds()[0];
    if ( id != null ) {
      return GUIResource.getInstance().getImagesSteps().get( id ).getAsBitmapForSize( shell.getDisplay(),
          ConstUI.ICON_SIZE, ConstUI.ICON_SIZE );
    }
    return null;
  }

  /**
   * Called when the user cancels the dialog.
   */
  private void cancel() {
    // The "stepname" variable will be the return value for the open() method.
    // Setting to null to indicate that dialog was cancelled.
    stepname = null;
    //may still need to set the extraction index
    meta.setExpandIndex(expandCombo.getSelectionIndex());
    // Restoring original "changed" flag on the met aobject
    meta.setChanged( changed );
    // close the SWT dialog window
    dispose();
  }


  private void ok() {
    if ( Utils.isEmpty( wStepname.getText() ) ) {
      return;
    }

    stepname = wStepname.getText(); // return value

    String expandInField = Const.NVL(expandCombo.getText(), null);
    String expandField = Const.NVL(wExpandOut.getText(), null);
    boolean isNer = wuseNer.getSelection();
    boolean addAll = wAddAll.getSelection();
    String lpPath = wLpOut.getText();
    String nerPath = wNerOut.getText();

    meta.setExpandInField(expandInField);
    meta.setExpandField(expandField);
    meta.setNer(isNer);
    meta.setAddAll(addAll);
    meta.setLpPath(lpPath);
    meta.setNerPath(nerPath);
    meta.setExpandIndex(expandCombo.getSelectionIndex());
    dispose();
  }
}