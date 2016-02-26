/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.governance.metadata.provider;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.governance.metadata.Base;
import org.wso2.carbon.governance.metadata.models.endpoint.HTTPEndpointV1;
import org.wso2.carbon.governance.metadata.provider.util.Util;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.governance.metadata.Constants;
import org.wso2.carbon.governance.metadata.exception.MetadataException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.*;

public class HTTPEndpointProviderV1 implements BaseProvider {

    private static final Log log = LogFactory.getLog(HTTPEndpointProviderV1.class);

    private final String mediaType;
    private final String versionMediaType;


    public HTTPEndpointProviderV1(String mediaType, String versionMediaType) {
        this.mediaType = mediaType;
        this.versionMediaType = versionMediaType;
    }

    public String getVersionMediaType() {
        return versionMediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    @Override
    public Resource buildResource(Base metadata, Resource resource) throws MetadataException {

        try {
            String content = getGeneratedMetadataOMElement(metadata).toStringWithConsume();
            resource.setContent(content);
            resource.setMediaType(metadata.getMediaType());
            resource.setUUID(metadata.getUUID());
        } catch (XMLStreamException e) {
            log.error("Xml stream exception occurred while building resource content " + e.getMessage());
            throw new MetadataException("Xml stream exception occurred while building resource content", e);
        } catch (RegistryException e) {
            throw new MetadataException(e.getMessage(), e);
        }
        return resource;
    }

    @Override
    public Resource updateResource(Base newMetadata, Resource resource) throws MetadataException {
        return buildResource(newMetadata, resource);
    }

    @Override
    public Base get(Resource resource, Registry registry) throws MetadataException {
        try {
            byte[] contentBytes = (byte[]) resource.getContent();
            OMElement root = Util.buildOMElement(contentBytes);
            Map<String, List<String>> propBag = Util.getPropertyBag(root);
            return getFilledBean(root, propBag, registry);
        } catch (RegistryException e) {
            throw new MetadataException("Error occurred while obtaining resource metadata content uuid = " + resource.getUUID(), e);
        }
    }


    private HTTPEndpointV1 getFilledBean(OMElement root, Map<String, List<String>> propBag, Registry registry) throws MetadataException {
        Map<String, List<String>> attributeMap = new HashMap<String, List<String>>();
        OMElement attributes = root.getFirstChildWithName(new QName(Constants.CONTENT_ATTRIBUTE_EL_ROOT_NAME));
        String uuid = attributes.getFirstChildWithName(new QName(Constants.ATTRIBUTE_UUID)).getText();
        String name = attributes.getFirstChildWithName(new QName(Constants.ATTRIBUTE_METADATA_NAME)).getText();
        Iterator itr = attributes.getChildren();
        while (itr.hasNext()) {
            OMElement el = (OMElement) itr.next();
            String key = el.getLocalName();
            List<String> valList = new ArrayList<String>();

            Iterator<OMElement> entries = el.getChildrenWithLocalName(Constants.ENTRY_KEY);
            if (entries.hasNext()) {
                while (entries.hasNext()) {
                    OMElement entry = entries.next();
                    valList.add(entry.getText());
                }
            } else {
                valList.add(el.getText());
            }

            attributeMap.put(key, valList);
        }
        return new HTTPEndpointV1(registry, name, uuid, propBag, attributeMap);
    }

    private void createAttributesContent(HTTPEndpointV1 httpEndpointV1, OMElement element) throws MetadataException {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement uuid = factory.createOMElement(new QName(Constants.ATTRIBUTE_UUID));
        uuid.setText(httpEndpointV1.getUUID());
        OMElement name = factory.createOMElement(new QName(Constants.ATTRIBUTE_METADATA_NAME));
        name.setText(httpEndpointV1.getName());

        OMElement endpoint = factory.createOMElement(new QName(HTTPEndpointV1.KEY_URL));
        endpoint.setText(httpEndpointV1.getUrl());

        element.addChild(uuid);
        element.addChild(name);
        element.addChild(endpoint);

        buildGenericAttributes(httpEndpointV1, element, factory);
    }

    private void buildGenericAttributes(HTTPEndpointV1 metadata, OMElement attributeRoot, OMFactory factory) {

        for (Map.Entry<String, List<String>> entry : metadata.getAttributeMap().entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            OMElement attEl = factory.createOMElement(new QName(key));
            if (value.size() == 1) {
                attEl.setText(value.get(0));
            } else {
                for (String s : value) {
                    OMElement entryEl = factory.createOMElement(new QName(Constants.ENTRY_KEY));
                    entryEl.setText(s);
                    attEl.addChild(entryEl);
                }
            }
            attributeRoot.addChild(attEl);
        }
    }

    private void createPropertiesContent(HTTPEndpointV1 httpEndpointV1, OMElement element) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        for (Map.Entry<String, List<String>> entry : httpEndpointV1.getPropertyBag().entrySet()) {
            if (entry.getValue() == null) continue;
            OMElement attribute = factory.createOMElement(new QName(entry.getKey()));
            attribute.setText(entry.getValue().get(0));
            element.addChild(attribute);
        }

    }

    private OMElement getGeneratedMetadataOMElement(Base metadata) throws MetadataException {
        OMElement root = Util.getContentRoot();
        OMElement attributes = Util.getAttributeRoot();
        OMElement properties = Util.getPropertyRoot();
        createAttributesContent((HTTPEndpointV1) metadata, attributes);
        createPropertiesContent((HTTPEndpointV1) metadata, properties);
        root.addChild(properties);
        root.addChild(attributes);
        return root;
    }

}
