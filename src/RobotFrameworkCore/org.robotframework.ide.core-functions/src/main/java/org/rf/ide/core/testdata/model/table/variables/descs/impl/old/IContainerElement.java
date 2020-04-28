/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.variables.descs.impl.old;

interface IContainerElement {

    boolean isComplex();


    ContainerElementType getType();


    String prettyPrint(int deepLevel);


    TextPosition getPosition();
}
