/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.openwire.tool;

import org.codehaus.jam.JAnnotation;
import org.codehaus.jam.JAnnotationValue;
import org.codehaus.jam.JClass;
import org.codehaus.jam.JProperty;

import java.io.*;
import java.util.*;

/**
 *
 * @version $Revision$
 */
public abstract class OpenWireCSharpMarshallingScript extends OpenWireJavaMarshallingScript {

    public Object run() {
        filePostFix = ".cs";
        if (destDir == null) {
            destDir = new File("../activemq-dotnet/src/main/csharp/ActiveMQ/OpenWire/V"+getOpenwireVersion());
        }
        
        return super.run();
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // This section is for the tight wire format encoding generator
    //////////////////////////////////////////////////////////////////////////////////////

    protected void generateTightUnmarshalBodyForProperty(PrintWriter out, JProperty property, JAnnotationValue size) {

        String propertyName = property.getSimpleName();
        String type = property.getType().getSimpleName();

        if (type.equals("boolean")) {
            out.println("        info." + propertyName + " = bs.ReadBoolean();");
        }
        else if (type.equals("byte")) {
            out.println("        info." + propertyName + " = dataIn.ReadByte();");
        }
        else if (type.equals("char")) {
            out.println("        info." + propertyName + " = dataIn.ReadChar();");
        }
        else if (type.equals("short")) {
            out.println("        info." + propertyName + " = dataIn.ReadInt16();");
        }
        else if (type.equals("int")) {
            out.println("        info." + propertyName + " = dataIn.ReadInt32();");
        }
        else if (type.equals("long")) {
            out.println("        info." + propertyName + " = TightUnmarshalLong(wireFormat, dataIn, bs);");
        }
        else if (type.equals("String")) {
            out.println("        info." + propertyName + " = TightUnmarshalString(dataIn, bs);");
        }
        else if (type.equals("byte[]") || type.equals("ByteSequence")) {
            if (size != null) {
                out.println("        info." + propertyName + " = ReadBytes(dataIn, " + size.asInt() + ");");
            }
            else {
                out.println("        info." + propertyName + " = ReadBytes(dataIn, bs.ReadBoolean());");
            }
        }
        else if (isThrowable(property.getType())) {
            out.println("        info." + propertyName + " = TightUnmarshalBrokerError(wireFormat, dataIn, bs);");
        }
        else if (isCachedProperty(property)) {
            out.println("        info." + propertyName + " = (" + type + ") TightUnmarshalCachedObject(wireFormat, dataIn, bs);");
        }
        else {
            out.println("        info." + propertyName + " = (" + type + ") TightUnmarshalNestedObject(wireFormat, dataIn, bs);");
        }
    }

    protected void generateTightUnmarshalBodyForArrayProperty(PrintWriter out, JProperty property, JAnnotationValue size) {
        JClass propertyType = property.getType();
        String arrayType = propertyType.getArrayComponentType().getSimpleName();
        String propertyName = property.getSimpleName();
        out.println();
        if (size != null) {
            out.println("        {");
            out.println("            " + arrayType + "[] value = new " + arrayType + "[" + size.asInt() + "];");
            out.println("            " + "for( int i=0; i < " + size.asInt() + "; i++ ) {");
            out.println("                value[i] = (" + arrayType + ") TightUnmarshalNestedObject(wireFormat,dataIn, bs);");
            out.println("            }");
            out.println("            info." + propertyName + " = value;");
            out.println("        }");
        }
        else {
            out.println("        if (bs.ReadBoolean()) {");
            out.println("            short size = dataIn.ReadInt16();");
            out.println("            " + arrayType + "[] value = new " + arrayType + "[size];");
            out.println("            for( int i=0; i < size; i++ ) {");
            out.println("                value[i] = (" + arrayType + ") TightUnmarshalNestedObject(wireFormat,dataIn, bs);");
            out.println("            }");
            out.println("            info." + propertyName + " = value;");
            out.println("        }");
            out.println("        else {");
            out.println("            info." + propertyName + " = null;");
            out.println("        }");
        }
    }

    protected int generateTightMarshal1Body(PrintWriter out) {
        List properties = getProperties();
        int baseSize = 0;
        for (Iterator iter = properties.iterator(); iter.hasNext();) {
            JProperty property = (JProperty) iter.next();
            JAnnotation annotation = property.getAnnotation("openwire:property");
            JAnnotationValue size = annotation.getValue("size");
            JClass propertyType = property.getType();
            String type = propertyType.getSimpleName();
            String getter = "info." + property.getSimpleName();

            if (type.equals("boolean")) {
                out.println("        bs.WriteBoolean(" + getter + ");");
            }
            else if (type.equals("byte")) {
                baseSize += 1;
            }
            else if (type.equals("char")) {
                baseSize += 2;
            }
            else if (type.equals("short")) {
                baseSize += 2;
            }
            else if (type.equals("int")) {
                baseSize += 4;
            }
            else if (type.equals("long")) {
                out.println("        rc += TightMarshalLong1(wireFormat, " + getter + ", bs);");
            }
            else if (type.equals("String")) {
                out.print("");
                out.println("        rc += TightMarshalString1(" + getter + ", bs);");
            }
            else if (type.equals("byte[]") || type.equals("ByteSequence")) {
                if (size == null) {
                    out.println("        bs.WriteBoolean(" + getter + "!=null);");
                    out.println("        rc += " + getter + "==null ? 0 : " + getter + ".Length+4;");
                }
                else {
                    baseSize += size.asInt();
                }
            }
            else if (propertyType.isArrayType()) {
                if (size != null) {
                    out.println("        rc += TightMarshalObjectArrayConstSize1(wireFormat, " + getter + ", bs, " + size.asInt() + ");");
                }
                else {
                    out.println("        rc += TightMarshalObjectArray1(wireFormat, " + getter + ", bs);");
                }
            }
            else if (isThrowable(propertyType)) {
                out.println("        rc += TightMarshalBrokerError1(wireFormat, " + getter + ", bs);");
            }
            else {
                if (isCachedProperty(property)) {
                    out.println("        rc += TightMarshalCachedObject1(wireFormat, (DataStructure)" + getter + ", bs);");
                }
                else {
                    out.println("        rc += TightMarshalNestedObject1(wireFormat, (DataStructure)" + getter + ", bs);");
                }
            }
        }
        return baseSize;
    }

    protected void generateTightMarshal2Body(PrintWriter out) {
        List properties = getProperties();
        for (Iterator iter = properties.iterator(); iter.hasNext();) {
            JProperty property = (JProperty) iter.next();
            JAnnotation annotation = property.getAnnotation("openwire:property");
            JAnnotationValue size = annotation.getValue("size");
            JClass propertyType = property.getType();
            String type = propertyType.getSimpleName();
            String getter = "info." + property.getSimpleName();

            if (type.equals("boolean")) {
                out.println("        bs.ReadBoolean();");
            }
            else if (type.equals("byte")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("char")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("short")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("int")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("long")) {
                out.println("        TightMarshalLong2(wireFormat, " + getter + ", dataOut, bs);");
            }
            else if (type.equals("String")) {
                out.println("        TightMarshalString2(" + getter + ", dataOut, bs);");
            }
            else if (type.equals("byte[]") || type.equals("ByteSequence")) {
                if (size != null) {
                    out.println("        dataOut.Write(" + getter + ", 0, " + size.asInt() + ");");
                }
                else {
                    out.println("        if(bs.ReadBoolean()) {");
                    out.println("           dataOut.Write(" + getter + ".Length);");
                    out.println("           dataOut.Write(" + getter + ");");
                    out.println("        }");
                }
            }
            else if (propertyType.isArrayType()) {
                if (size != null) {
                    out.println("        TightMarshalObjectArrayConstSize2(wireFormat, " + getter + ", dataOut, bs, " + size.asInt() + ");");
                }
                else {
                    out.println("        TightMarshalObjectArray2(wireFormat, " + getter + ", dataOut, bs);");
                }
            }
            else if (isThrowable(propertyType)) {
                out.println("        TightMarshalBrokerError2(wireFormat, " + getter + ", dataOut, bs);");
            }
            else {
                if (isCachedProperty(property)) {
                    out.println("        TightMarshalCachedObject2(wireFormat, (DataStructure)" + getter + ", dataOut, bs);");
                }
                else {
                    out.println("        TightMarshalNestedObject2(wireFormat, (DataStructure)" + getter + ", dataOut, bs);");
                }
            }
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // This section is for the loose wire format encoding generator
    //////////////////////////////////////////////////////////////////////////////////////
    
    protected void generateLooseUnmarshalBodyForProperty(PrintWriter out, JProperty property, JAnnotationValue size) {

        String propertyName = property.getSimpleName();
        String type = property.getType().getSimpleName();

        if (type.equals("boolean")) {
            out.println("        info." + propertyName + " = dataIn.ReadBoolean();");
        }
        else if (type.equals("byte")) {
            out.println("        info." + propertyName + " = dataIn.ReadByte();");
        }
        else if (type.equals("char")) {
            out.println("        info." + propertyName + " = dataIn.ReadChar();");
        }
        else if (type.equals("short")) {
            out.println("        info." + propertyName + " = dataIn.ReadInt16();");
        }
        else if (type.equals("int")) {
            out.println("        info." + propertyName + " = dataIn.ReadInt32();");
        }
        else if (type.equals("long")) {
            out.println("        info." + propertyName + " = LooseUnmarshalLong(wireFormat, dataIn);");
        }
        else if (type.equals("String")) {
            out.println("        info." + propertyName + " = LooseUnmarshalString(dataIn);");
        }
        else if (type.equals("byte[]") || type.equals("ByteSequence")) {
            if (size != null) {
                out.println("        info." + propertyName + " = ReadBytes(dataIn, " + size.asInt() + ");");
            }
            else {
                out.println("        info." + propertyName + " = ReadBytes(dataIn, dataIn.ReadBoolean());");
            }
        }
        else if (isThrowable(property.getType())) {
            out.println("        info." + propertyName + " = LooseUnmarshalBrokerError(wireFormat, dataIn);");
        }
        else if (isCachedProperty(property)) {
            out.println("        info." + propertyName + " = (" + type + ") LooseUnmarshalCachedObject(wireFormat, dataIn);");
        }
        else {
            out.println("        info." + propertyName + " = (" + type + ") LooseUnmarshalNestedObject(wireFormat, dataIn);");
        }
    }

    protected void generateLooseUnmarshalBodyForArrayProperty(PrintWriter out, JProperty property, JAnnotationValue size) {
        JClass propertyType = property.getType();
        String arrayType = propertyType.getArrayComponentType().getSimpleName();
        String propertyName = property.getSimpleName();
        out.println();
        if (size != null) {
            out.println("        {");
            out.println("            " + arrayType + "[] value = new " + arrayType + "[" + size.asInt() + "];");
            out.println("            " + "for( int i=0; i < " + size.asInt() + "; i++ ) {");
            out.println("                value[i] = (" + arrayType + ") LooseUnmarshalNestedObject(wireFormat,dataIn);");
            out.println("            }");
            out.println("            info." + propertyName + " = value;");
            out.println("        }");
        }
        else {
            out.println("        if (dataIn.ReadBoolean()) {");
            out.println("            short size = dataIn.ReadInt16();");
            out.println("            " + arrayType + "[] value = new " + arrayType + "[size];");
            out.println("            for( int i=0; i < size; i++ ) {");
            out.println("                value[i] = (" + arrayType + ") LooseUnmarshalNestedObject(wireFormat,dataIn);");
            out.println("            }");
            out.println("            info." + propertyName + " = value;");
            out.println("        }");
            out.println("        else {");
            out.println("            info." + propertyName + " = null;");
            out.println("        }");
        }
    }


    protected void generateLooseMarshalBody(PrintWriter out) {
        List properties = getProperties();
        for (Iterator iter = properties.iterator(); iter.hasNext();) {
            JProperty property = (JProperty) iter.next();
            JAnnotation annotation = property.getAnnotation("openwire:property");
            JAnnotationValue size = annotation.getValue("size");
            JClass propertyType = property.getType();
            String type = propertyType.getSimpleName();
            String getter = "info." + property.getSimpleName();

            if (type.equals("boolean")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("byte")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("char")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("short")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("int")) {
                out.println("        dataOut.Write(" + getter + ");");
            }
            else if (type.equals("long")) {
                out.println("        LooseMarshalLong(wireFormat, " + getter + ", dataOut);");
            }
            else if (type.equals("String")) {
                out.println("        LooseMarshalString(" + getter + ", dataOut);");
            }
            else if (type.equals("byte[]") || type.equals("ByteSequence")) {
                if (size != null) {
                    out.println("        dataOut.Write(" + getter + ", 0, " + size.asInt() + ");");
                }
                else {
                    out.println("        dataOut.Write(" + getter + "!=null);");
                    out.println("        if(" + getter + "!=null) {");
                    out.println("           dataOut.Write(" + getter + ".Length);");
                    out.println("           dataOut.Write(" + getter + ");");
                    out.println("        }");
                }
            }
            else if (propertyType.isArrayType()) {
                if (size != null) {
                    out.println("        LooseMarshalObjectArrayConstSize(wireFormat, " + getter + ", dataOut, " + size.asInt() + ");");
                }
                else {
                    out.println("        LooseMarshalObjectArray(wireFormat, " + getter + ", dataOut);");
                }
            }
            else if (isThrowable(propertyType)) {
                out.println("        LooseMarshalBrokerError(wireFormat, " + getter + ", dataOut);");
            }
            else {
                if (isCachedProperty(property)) {
                    out.println("        LooseMarshalCachedObject(wireFormat, (DataStructure)" + getter + ", dataOut);");
                }
                else {
                    out.println("        LooseMarshalNestedObject(wireFormat, (DataStructure)" + getter + ", dataOut);");
                }
            }
        }
    }

}
