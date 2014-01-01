/**
 * $Revision: 1722 $
 * $Date: 2005-07-28 15:19:16 -0700 (Thu, 28 Jul 2005) $
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.plugin.rooomservice;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.forms.DataForm;
import org.jivesoftware.openfire.forms.FormField;
import org.jivesoftware.openfire.forms.spi.XDataFormImpl;
import org.jivesoftware.openfire.forms.spi.XFormFieldImpl;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.muc.NotAllowedException;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.StringUtils;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that allows the administration of users via HTTP requests.
 *
 * @author Justin Hunt
 */
public class RoomServicePlugin implements Plugin, PropertyEventListener {
    public static final Logger Log = LoggerFactory.getLogger(RoomServicePlugin.class);

    private MultiUserChatManager chatManager;
    private XMPPServer server;

    private String secret;
    private boolean enabled;
    private Collection<String> allowedIPs;

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        server = XMPPServer.getInstance();
        chatManager = server.getMultiUserChatManager();

        secret = JiveGlobals.getProperty("plugin.roomservice.secret", "");
        // If no secret key has been assigned to the user service yet, assign a random one.
        if (secret.equals("")){
            secret = StringUtils.randomString(8);
            setSecret(secret);
        }

        // See if the service is enabled or not.
        enabled = JiveGlobals.getBooleanProperty("plugin.roomservice.enabled", false);

        // Get the list of IP addresses that can use this service. An empty list means that this filter is disabled.
        allowedIPs = StringUtils.stringToCollection(JiveGlobals.getProperty("plugin.roomservice.allowedIPs", ""));

        // Listen to system property events
        PropertyEventDispatcher.addListener(this);
    }

    public void destroyPlugin() {
        chatManager = null;
        // Stop listening to system property events
        PropertyEventDispatcher.removeListener(this);
    }

    public void createChat(String jid, String subdomain, String roomName) throws NotAllowedException {
        MultiUserChatService multiUserChatService = chatManager.getMultiUserChatService(subdomain);
        MUCRoom room = multiUserChatService.getChatRoom(roomName, new JID(jid));

        try {
            FormField field;
            XDataFormImpl dataForm = new XDataFormImpl(DataForm.TYPE_SUBMIT);

            field = new XFormFieldImpl("FORM_TYPE");
            field.setType(FormField.TYPE_HIDDEN);
            field.addValue("http://jabber.org/protocol/muc#roomconfig");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_roomname");
            field.addValue(roomName);
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_roomdesc");
            field.addValue(roomName);
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_changesubject");
            field.addValue("0");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_maxusers");
            field.addValue("0");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_presencebroadcast");
            field.addValue("moderator");
            field.addValue("participant");
            field.addValue("visitor");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_publicroom");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_persistentroom");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_moderatedroom");
            field.addValue("0");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_membersonly");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_allowinvites");
            field.addValue("0");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_passwordprotectedroom");
            field.addValue("0");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_roomsecret");
            field.addValue("");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_whois");
            field.addValue("moderator");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_enablelogging");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("x-muc#roomconfig_reservednick");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("x-muc#roomconfig_canchangenick");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("x-muc#roomconfig_registration");
            field.addValue("1");
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_roomadmins");
            for (JID adminJID : room.getAdmins()) {
                field.addValue(adminJID.toString());
            }
            dataForm.addField(field);

            field = new XFormFieldImpl("muc#roomconfig_roomowners");
            for (JID ownerJID : room.getOwners()) {
                field.addValue(ownerJID.toString());
            }
            dataForm.addField(field);

            IQ iq = new IQ(IQ.Type.set);
            Element element = iq.setChildElement("query", "http://jabber.org/protocol/muc#owner");
            element.add(dataForm.asXMLElement());
            room.getIQOwnerHandler().handleIQ(iq, room.getRole());
        } catch (Exception e) {
            Log.error("exception: " + e);
        }
    }

    public void deleteChat(String jid, String subdomain, String roomName) {
        MultiUserChatService multiUserChatService = chatManager.getMultiUserChatService(subdomain);
        multiUserChatService.removeChatRoom(roomName);
    }

    /**
     * Returns the secret key that only valid requests should know.
     *
     * @return the secret key.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the secret key that grants permission to use the userservice.
     *
     * @param secret the secret key.
     */
    public void setSecret(String secret) {
        JiveGlobals.setProperty("plugin.roomservice.secret", secret);
        this.secret = secret;
    }

    public Collection<String> getAllowedIPs() {
        return allowedIPs;
    }

    public void setAllowedIPs(Collection<String> allowedIPs) {
        JiveGlobals.setProperty("plugin.roomservice.allowedIPs", StringUtils.collectionToString(allowedIPs));
        this.allowedIPs = allowedIPs;
    }

    /**
     * Returns true if the user service is enabled. If not enabled, it will not accept
     * requests to create new accounts.
     *
     * @return true if the user service is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the user service. If not enabled, it will not accept
     * requests to create new accounts.
     *
     * @param enabled true if the user service should be enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        JiveGlobals.setProperty("plugin.roomservice.enabled",  enabled ? "true" : "false");
    }

    public void propertySet(String property, Map<String, Object> params) {
        if (property.equals("plugin.roomservice.secret")) {
            this.secret = (String)params.get("value");
        }
        else if (property.equals("plugin.roomservice.enabled")) {
            this.enabled = Boolean.parseBoolean((String)params.get("value"));
        }
        else if (property.equals("plugin.roomservice.allowedIPs")) {
            this.allowedIPs = StringUtils.stringToCollection((String)params.get("value"));
        }
    }

    public void propertyDeleted(String property, Map<String, Object> params) {
        if (property.equals("plugin.roomservice.secret")) {
            this.secret = "";
        }
        else if (property.equals("plugin.roomservice.enabled")) {
            this.enabled = false;
        }
        else if (property.equals("plugin.roomservice.allowedIPs")) {
            this.allowedIPs = Collections.emptyList();
        }
    }

    public void xmlPropertySet(String property, Map<String, Object> params) {
        // Do nothing
    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {
        // Do nothing
    }
}
