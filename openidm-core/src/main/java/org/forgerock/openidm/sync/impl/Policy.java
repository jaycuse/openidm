/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

// Java Standard Edition
import java.util.Map;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// OpenIDM
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.sync.SynchronizationException;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class Policy {

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(Policy.class);

    /** TODO: Description. */
    private final SynchronizationService service;

    /** TODO: Description. */
    private final Situation situation;

    /** TODO: Description. */
    private final Action action;

    /** TODO: Description. */
    private final Script script;

    /**
     * TODO: Description.
     *
     * @param service
     * @param config TODO.
     * @throws JsonNodeException TODO>
     */
    public Policy(SynchronizationService service, JsonNode config) throws JsonNodeException {
        this.service = service;
        situation = config.get("situation").required().asEnum(Situation.class);
        JsonNode action = config.get("action").required();
        if (action.isString()) {
            this.action = action.asEnum(Action.class);
            this.script = null;
        } else {
            this.action = null;
            this.script = Scripts.newInstance(action);
        }
    }

    /**
     * TODO: Description.
     * @return
     */
    public Situation getSituation() {
        return situation;
    }

    /**
     * TODO: Description.
     *
     * @param source
     * @param target
     * @return TODO.
     * @throws SynchronizationException TODO.
     */
    public Action getAction(JsonNode source, JsonNode target) throws SynchronizationException {
        Action result = null;
        if (action != null) { // static action specified
            result = action;
        } else if (script != null) { // action is dynamically determine 
            Map<String, Object> scope = service.newScope();
            if (source != null) {
                scope.put("source", source.copy().asMap());
            }
            if (target != null) {
                scope.put("target", target.copy().asMap());
            }
            try {
                result = Enum.valueOf(Action.class, script.exec(scope).toString());
            } catch (NullPointerException npe) {
                throw new SynchronizationException("action script returned null value");
            } catch (IllegalArgumentException iae) {
                throw new SynchronizationException("action script returned invalid action");
            } catch (ScriptException se) {
                LOGGER.debug("action script encountered exception", se);
                throw new SynchronizationException(se);
            }
        }
        return result;
    }
}
