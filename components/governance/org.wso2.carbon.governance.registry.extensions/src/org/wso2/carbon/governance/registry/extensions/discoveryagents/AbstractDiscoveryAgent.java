/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.governance.registry.extensions.discoveryagents;

import java.util.Map;

/**
 * Instead of implementing DiscoveryAgent it's obvious to extend AbstractDiscoveryAgent class as it provide number of
 * helper methods.
 */
public abstract class AbstractDiscoveryAgent implements DiscoveryAgent {

    @Override
    public void init(Map properties) {

    }

    @Override
    public void close(Map properties) {

    }


}
