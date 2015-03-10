/**
 * ResponseHeader.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mct.provisioning;

public class ResponseHeader  implements java.io.Serializable {
    private int responseCode;

    private java.lang.String responseName;

    private java.lang.String responseDesc;

    public ResponseHeader() {
    }

    public ResponseHeader(
           int responseCode,
           java.lang.String responseName,
           java.lang.String responseDesc) {
           this.responseCode = responseCode;
           this.responseName = responseName;
           this.responseDesc = responseDesc;
    }


    /**
     * Gets the responseCode value for this ResponseHeader.
     * 
     * @return responseCode
     */
    public int getResponseCode() {
        return responseCode;
    }


    /**
     * Sets the responseCode value for this ResponseHeader.
     * 
     * @param responseCode
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }


    /**
     * Gets the responseName value for this ResponseHeader.
     * 
     * @return responseName
     */
    public java.lang.String getResponseName() {
        return responseName;
    }


    /**
     * Sets the responseName value for this ResponseHeader.
     * 
     * @param responseName
     */
    public void setResponseName(java.lang.String responseName) {
        this.responseName = responseName;
    }


    /**
     * Gets the responseDesc value for this ResponseHeader.
     * 
     * @return responseDesc
     */
    public java.lang.String getResponseDesc() {
        return responseDesc;
    }


    /**
     * Sets the responseDesc value for this ResponseHeader.
     * 
     * @param responseDesc
     */
    public void setResponseDesc(java.lang.String responseDesc) {
        this.responseDesc = responseDesc;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ResponseHeader)) return false;
        ResponseHeader other = (ResponseHeader) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.responseCode == other.getResponseCode() &&
            ((this.responseName==null && other.getResponseName()==null) || 
             (this.responseName!=null &&
              this.responseName.equals(other.getResponseName()))) &&
            ((this.responseDesc==null && other.getResponseDesc()==null) || 
             (this.responseDesc!=null &&
              this.responseDesc.equals(other.getResponseDesc())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += getResponseCode();
        if (getResponseName() != null) {
            _hashCode += getResponseName().hashCode();
        }
        if (getResponseDesc() != null) {
            _hashCode += getResponseDesc().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ResponseHeader.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://10.8.39.106/mct/provisionapi/wsprovisioning.php?wsdl", "ResponseHeader"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ResponseCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ResponseName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseDesc");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ResponseDesc"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
