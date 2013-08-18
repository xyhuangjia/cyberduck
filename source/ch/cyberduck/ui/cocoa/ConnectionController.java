package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.ftp.FTPConnectMode;
import ch.cyberduck.core.threading.AbstractBackgroundAction;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSEnumerator;
import ch.cyberduck.ui.cocoa.foundation.NSNotification;
import ch.cyberduck.ui.cocoa.foundation.NSNotificationCenter;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSString;
import ch.cyberduck.ui.resources.IconCacheFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSInteger;

/**
 * @version $Id$
 */
public class ConnectionController extends SheetController {
    private static Logger log = Logger.getLogger(ConnectionController.class);

    @Override
    protected void invalidate() {
        hostField.setDelegate(null);
        hostField.setDataSource(null);
        super.invalidate();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public ConnectionController(final WindowController parent) {
        super(parent);
        this.loadBundle();
    }

    @Override
    protected String getBundleName() {
        return "Connection";
    }

    @Override
    public void awakeFromNib() {
        this.protocolSelectionDidChange(null);
        this.setState(toggleOptionsButton, Preferences.instance().getBoolean("connection.toggle.options"));
        super.awakeFromNib();
    }

    @Override
    public void beginSheet() {
        // Reset password input
        passField.setStringValue(StringUtils.EMPTY);
        super.beginSheet();
    }

    @Override
    protected double getMaxWindowHeight() {
        return this.window().frame().size.height.doubleValue();
    }

    @Outlet
    private NSPopUpButton protocolPopup;

    public void setProtocolPopup(NSPopUpButton protocolPopup) {
        this.protocolPopup = protocolPopup;
        this.protocolPopup.setEnabled(true);
        this.protocolPopup.setTarget(this.id());
        this.protocolPopup.setAction(Foundation.selector("protocolSelectionDidChange:"));
        this.protocolPopup.removeAllItems();
        for(Protocol protocol : ProtocolFactory.getKnownProtocols()) {
            final String title = protocol.getDescription();
            this.protocolPopup.addItemWithTitle(title);
            final NSMenuItem item = this.protocolPopup.itemWithTitle(title);
            item.setRepresentedObject(String.valueOf(protocol.hashCode()));
            item.setImage(IconCacheFactory.<NSImage>get().iconNamed(protocol.icon(), 16));
        }
        final Protocol defaultProtocol
                = ProtocolFactory.forName(Preferences.instance().getProperty("connection.protocol.default"));
        this.protocolPopup.selectItemAtIndex(
                protocolPopup.indexOfItemWithRepresentedObject(String.valueOf(defaultProtocol.hashCode()))
        );
    }

    public void protocolSelectionDidChange(final NSPopUpButton sender) {
        log.debug("protocolSelectionDidChange:" + sender);
        final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
        portField.setIntValue(protocol.getDefaultPort());
        portField.setEnabled(protocol.isPortConfigurable());
        if(!protocol.isHostnameConfigurable()) {
            hostField.setStringValue(protocol.getDefaultHostname());
            hostField.setEnabled(false);
            pathField.setEnabled(true);
        }
        else {
            if(!hostField.isEnabled()) {
                // Was previously configured with a static configuration
                hostField.setStringValue(protocol.getDefaultHostname());
            }
            if(!pathField.isEnabled()) {
                // Was previously configured with a static configuration
                pathField.setStringValue(StringUtils.EMPTY);
            }
            if(StringUtils.isNotBlank(protocol.getDefaultHostname())) {
                // Prefill with default hostname
                hostField.setStringValue(protocol.getDefaultHostname());
            }
            usernameField.setEnabled(true);
            hostField.setEnabled(true);
            pathField.setEnabled(true);
            usernameField.cell().setPlaceholderString(StringUtils.EMPTY);
            passField.cell().setPlaceholderString(StringUtils.EMPTY);
        }
        hostField.cell().setPlaceholderString(protocol.getDefaultHostname());
        usernameField.cell().setPlaceholderString(protocol.getUsernamePlaceholder());
        passField.cell().setPlaceholderString(protocol.getPasswordPlaceholder());
        connectmodePopup.setEnabled(protocol.getType() == Protocol.Type.ftp);
        if(!protocol.isEncodingConfigurable()) {
            encodingPopup.selectItemWithTitle(DEFAULT);
        }
        encodingPopup.setEnabled(protocol.isEncodingConfigurable());
        anonymousCheckbox.setEnabled(protocol.isAnonymousConfigurable());

        this.updateIdentity();
        this.updateURLLabel();

        this.reachable();
    }

    /**
     * Update Private Key selection
     */
    private void updateIdentity() {
        final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
        pkCheckbox.setEnabled(protocol.getType() == Protocol.Type.ssh);
        if(StringUtils.isNotEmpty(hostField.stringValue())) {
            final Credentials credentials = new Credentials();
            CredentialsConfiguratorFactory.get(protocol).configure(credentials, hostField.stringValue());
            if(credentials.isPublicKeyAuthentication()) {
                // No previously manually selected key
                pkLabel.setStringValue(credentials.getIdentity().getAbbreviatedPath());
                pkCheckbox.setState(NSCell.NSOnState);
            }
            else {
                pkCheckbox.setState(NSCell.NSOffState);
                pkLabel.setStringValue(LocaleFactory.localizedString("No private key selected"));
            }
            if(StringUtils.isNotBlank(credentials.getUsername())) {
                usernameField.setStringValue(credentials.getUsername());
            }
        }
    }

    private NSComboBox hostField;
    private ProxyController hostFieldModel = new HostFieldModel();

    public void setHostPopup(NSComboBox hostPopup) {
        this.hostField = hostPopup;
        this.hostField.setTarget(this.id());
        this.hostField.setAction(Foundation.selector("hostPopupSelectionDidChange:"));
        this.hostField.setUsesDataSource(true);
        this.hostField.setDataSource(hostFieldModel.id());
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("hostFieldTextDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.hostField);
    }

    private static class HostFieldModel extends ProxyController implements NSComboBox.DataSource {
        @Override
        public NSInteger numberOfItemsInComboBox(final NSComboBox sender) {
            return new NSInteger(BookmarkCollection.defaultCollection().size());
        }

        @Override
        public NSObject comboBox_objectValueForItemAtIndex(final NSComboBox sender, final NSInteger row) {
            return NSString.stringWithString(BookmarkCollection.defaultCollection().get(row.intValue()).getNickname());
        }
    }

    @Action
    public void hostPopupSelectionDidChange(final NSControl sender) {
        String input = sender.stringValue();
        if(StringUtils.isBlank(input)) {
            return;
        }
        input = input.trim();
        // First look for equivalent bookmarks
        for(Host h : BookmarkCollection.defaultCollection()) {
            if(h.getNickname().equals(input)) {
                this.hostChanged(h);
                break;
            }
        }
    }

    public void hostFieldTextDidChange(final NSNotification sender) {
        if(ProtocolFactory.isURL(hostField.stringValue())) {
            final Host parsed = HostParser.parse(hostField.stringValue());
            this.hostChanged(parsed);
        }
        else {
            this.updateURLLabel();
            this.updateIdentity();
            this.reachable();
        }
    }

    private void hostChanged(final Host host) {
        this.updateField(hostField, host.getHostname());
        this.protocolPopup.selectItemAtIndex(
                protocolPopup.indexOfItemWithRepresentedObject(String.valueOf(host.getProtocol().hashCode()))
        );
        this.updateField(portField, String.valueOf(host.getPort()));
        this.updateField(usernameField, host.getCredentials().getUsername());
        this.updateField(pathField, host.getDefaultPath());
        anonymousCheckbox.setState(host.getCredentials().isAnonymousLogin() ? NSCell.NSOnState : NSCell.NSOffState);
        this.anonymousCheckboxClicked(anonymousCheckbox);
        if(host.getCredentials().isPublicKeyAuthentication()) {
            pkCheckbox.setState(NSCell.NSOnState);
            pkLabel.setStringValue(host.getCredentials().getIdentity().getAbbreviatedPath());
        }
        else {
            this.updateIdentity();
        }
        this.updateURLLabel();
        this.readPasswordFromKeychain();
        this.reachable();
    }

    /**
     * Run the connection reachability test in the background
     */
    private void reachable() {
        final String hostname = hostField.stringValue();
        if(StringUtils.isNotBlank(hostname)) {
            this.background(new AbstractBackgroundAction<Boolean>() {
                boolean reachable = false;

                @Override
                public Boolean run() throws BackgroundException {
                    return reachable = ReachabilityFactory.get().isReachable(new Host(hostname));
                }

                @Override
                public void cleanup() {
                    alertIcon.setEnabled(!reachable);
                    alertIcon.setImage(reachable ? null : IconCacheFactory.<NSImage>get().iconNamed("alert.tiff"));
                }
            });
        }
        else {
            alertIcon.setImage(IconCacheFactory.<NSImage>get().iconNamed("alert.tiff"));
            alertIcon.setEnabled(false);
        }
    }

    @Outlet
    private NSButton alertIcon;

    public void setAlertIcon(NSButton alertIcon) {
        this.alertIcon = alertIcon;
        this.alertIcon.setTarget(this.id());
        this.alertIcon.setAction(Foundation.selector("launchNetworkAssistant:"));
    }

    @Action
    public void launchNetworkAssistant(final NSButton sender) {
        ReachabilityFactory.get().diagnose(HostParser.parse(urlLabel.stringValue()));
    }

    @Outlet
    private NSTextField pathField;

    public void setPathField(NSTextField pathField) {
        this.pathField = pathField;
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("pathInputDidEndEditing:"),
                NSControl.NSControlTextDidEndEditingNotification,
                this.pathField);
    }

    public void pathInputDidEndEditing(final NSNotification sender) {
        this.updateURLLabel();
    }

    @Outlet
    private NSTextField portField;

    public void setPortField(NSTextField portField) {
        this.portField = portField;
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("portFieldTextDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.portField);
    }

    public void portFieldTextDidChange(final NSNotification sender) {
        if(StringUtils.isBlank(this.portField.stringValue())) {
            final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
            this.portField.setIntValue(protocol.getDefaultPort());
        }
        this.updateURLLabel();
        this.reachable();
    }

    @Outlet
    private NSTextField usernameField;

    public void setUsernameField(NSTextField usernameField) {
        this.usernameField = usernameField;
        this.usernameField.setStringValue(Preferences.instance().getProperty("connection.login.name"));
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("usernameFieldTextDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.usernameField);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("usernameFieldTextDidEndEditing:"),
                NSControl.NSControlTextDidEndEditingNotification,
                this.usernameField);
    }

    public void usernameFieldTextDidChange(final NSNotification sender) {
        this.updateURLLabel();
    }

    public void usernameFieldTextDidEndEditing(final NSNotification sender) {
        this.readPasswordFromKeychain();
    }

    @Outlet
    private NSTextField passField;

    public void setPassField(NSTextField passField) {
        this.passField = passField;
    }

    @Outlet
    private NSTextField pkLabel;

    public void setPkLabel(NSTextField pkLabel) {
        this.pkLabel = pkLabel;
        this.pkLabel.setStringValue(LocaleFactory.localizedString("No private key selected"));
        this.pkLabel.setTextColor(NSColor.disabledControlTextColor());
    }

    @Outlet
    private NSButton keychainCheckbox;

    public void setKeychainCheckbox(NSButton keychainCheckbox) {
        this.keychainCheckbox = keychainCheckbox;
        this.keychainCheckbox.setState(Preferences.instance().getBoolean("connection.login.useKeychain")
                && Preferences.instance().getBoolean("connection.login.addKeychain") ? NSCell.NSOnState : NSCell.NSOffState);
        this.keychainCheckbox.setTarget(this.id());
        this.keychainCheckbox.setAction(Foundation.selector("keychainCheckboxClicked:"));
    }

    public void keychainCheckboxClicked(final NSButton sender) {
        final boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("connection.login.addKeychain", enabled);
    }

    @Outlet
    private NSButton anonymousCheckbox;

    public void setAnonymousCheckbox(NSButton anonymousCheckbox) {
        this.anonymousCheckbox = anonymousCheckbox;
        this.anonymousCheckbox.setTarget(this.id());
        this.anonymousCheckbox.setAction(Foundation.selector("anonymousCheckboxClicked:"));
        this.anonymousCheckbox.setState(NSCell.NSOffState);
    }

    @Action
    public void anonymousCheckboxClicked(final NSButton sender) {
        if(sender.state() == NSCell.NSOnState) {
            this.usernameField.setEnabled(false);
            this.usernameField.setStringValue(Preferences.instance().getProperty("connection.login.anon.name"));
            this.passField.setEnabled(false);
            this.passField.setStringValue(StringUtils.EMPTY);
        }
        if(sender.state() == NSCell.NSOffState) {
            this.usernameField.setEnabled(true);
            this.usernameField.setStringValue(Preferences.instance().getProperty("connection.login.name"));
            this.passField.setEnabled(true);
        }
        this.updateURLLabel();
    }

    @Outlet
    private NSButton pkCheckbox;

    public void setPkCheckbox(NSButton pkCheckbox) {
        this.pkCheckbox = pkCheckbox;
        this.pkCheckbox.setTarget(this.id());
        this.pkCheckbox.setAction(Foundation.selector("pkCheckboxSelectionDidChange:"));
        this.pkCheckbox.setState(NSCell.NSOffState);
    }

    private NSOpenPanel publicKeyPanel;

    @Action
    public void pkCheckboxSelectionDidChange(final NSButton sender) {
        log.debug("pkCheckboxSelectionDidChange");
        if(sender.state() == NSCell.NSOnState) {
            publicKeyPanel = NSOpenPanel.openPanel();
            publicKeyPanel.setCanChooseDirectories(false);
            publicKeyPanel.setCanChooseFiles(true);
            publicKeyPanel.setAllowsMultipleSelection(false);
            publicKeyPanel.setMessage(LocaleFactory.localizedString("Select the private key in PEM or PuTTY format", "Credentials"));
            publicKeyPanel.setPrompt(LocaleFactory.localizedString("Choose"));
            publicKeyPanel.beginSheetForDirectory(LocalFactory.createLocal("~/.ssh").getAbsolute(),
                    null, this.window(), this.id(),
                    Foundation.selector("pkSelectionPanelDidEnd:returnCode:contextInfo:"), null);
        }
        else {
            passField.setEnabled(true);
            pkCheckbox.setState(NSCell.NSOffState);
            pkLabel.setStringValue(LocaleFactory.localizedString("No private key selected"));
            pkLabel.setTextColor(NSColor.disabledControlTextColor());
        }
    }

    public void pkSelectionPanelDidEnd_returnCode_contextInfo(NSOpenPanel window, int returncode, ID contextInfo) {
        if(NSPanel.NSOKButton == returncode) {
            NSArray selected = window.filenames();
            final NSEnumerator enumerator = selected.objectEnumerator();
            NSObject next;
            while(null != (next = enumerator.nextObject())) {
                pkLabel.setStringValue(LocalFactory.createLocal(
                        Rococoa.cast(next, NSString.class).toString()).getAbbreviatedPath());
                pkLabel.setTextColor(NSColor.textColor());
            }
            passField.setEnabled(false);
        }
        if(NSPanel.NSCancelButton == returncode) {
            passField.setEnabled(true);
            pkCheckbox.setState(NSCell.NSOffState);
            pkLabel.setStringValue(LocaleFactory.localizedString("No private key selected"));
            pkLabel.setTextColor(NSColor.disabledControlTextColor());
        }
        publicKeyPanel = null;
    }

    @Outlet
    private NSTextField urlLabel;

    public void setUrlLabel(NSTextField urlLabel) {
        this.urlLabel = urlLabel;
        this.urlLabel.setAllowsEditingTextAttributes(true);
        this.urlLabel.setSelectable(true);
    }

    @Outlet
    private NSPopUpButton encodingPopup;

    public void setEncodingPopup(NSPopUpButton encodingPopup) {
        this.encodingPopup = encodingPopup;
        this.encodingPopup.setEnabled(true);
        this.encodingPopup.removeAllItems();
        this.encodingPopup.addItemWithTitle(DEFAULT);
        this.encodingPopup.menu().addItem(NSMenuItem.separatorItem());
        this.encodingPopup.addItemsWithTitles(NSArray.arrayWithObjects(MainController.availableCharsets()));
        this.encodingPopup.selectItemWithTitle(DEFAULT);
    }

    @Outlet
    private NSPopUpButton connectmodePopup;

    private static final String CONNECTMODE_ACTIVE = LocaleFactory.localizedString("Active");
    private static final String CONNECTMODE_PASSIVE = LocaleFactory.localizedString("Passive");

    public void setConnectmodePopup(NSPopUpButton connectmodePopup) {
        this.connectmodePopup = connectmodePopup;
        this.connectmodePopup.removeAllItems();
        this.connectmodePopup.addItemWithTitle(DEFAULT);
        this.connectmodePopup.menu().addItem(NSMenuItem.separatorItem());
        this.connectmodePopup.addItemWithTitle(CONNECTMODE_ACTIVE);
        this.connectmodePopup.addItemWithTitle(CONNECTMODE_PASSIVE);
        this.connectmodePopup.selectItemWithTitle(DEFAULT);
    }

    @Outlet
    private NSButton toggleOptionsButton;

    public void setToggleOptionsButton(NSButton b) {
        this.toggleOptionsButton = b;
    }

    /**
     * Updating the password field with the actual password if any
     * is avaialble for this hostname
     */
    public void readPasswordFromKeychain() {
        if(Preferences.instance().getBoolean("connection.login.useKeychain")) {
            if(StringUtils.isBlank(hostField.stringValue())) {
                return;
            }
            if(StringUtils.isBlank(portField.stringValue())) {
                return;
            }
            if(StringUtils.isBlank(usernameField.stringValue())) {
                return;
            }
            final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
            this.updateField(this.passField, PasswordStoreFactory.get().getPassword(protocol.getScheme(),
                    portField.intValue(),
                    hostField.stringValue(), usernameField.stringValue()));
        }
    }

    /**
     */
    private void updateURLLabel() {
        if(StringUtils.isNotBlank(hostField.stringValue())) {
            final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
            final String url = protocol.getScheme() + "://" + usernameField.stringValue()
                    + "@" + hostField.stringValue() + ":" + portField.intValue()
                    + PathNormalizer.normalize(pathField.stringValue());
            urlLabel.setAttributedStringValue(HyperlinkAttributedStringFactory.create(url));
        }
        else {
            urlLabel.setStringValue(StringUtils.EMPTY);
        }
    }

    public void helpButtonClicked(final ID sender) {
        final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
        StringBuilder site = new StringBuilder(Preferences.instance().getProperty("website.help"));
        site.append("/").append(protocol.getProvider());
        openUrl(site.toString());
    }

    @Override
    protected boolean validateInput() {
        if(StringUtils.isBlank(hostField.stringValue())) {
            return false;
        }
        if(StringUtils.isBlank(usernameField.stringValue())) {
            return false;
        }
        return true;
    }

    @Override
    public void callback(final int returncode) {
        if(returncode == DEFAULT_OPTION) {
            this.window().endEditingFor(null);
            final Protocol protocol = ProtocolFactory.forName(protocolPopup.selectedItem().representedObject());
            Host host = new Host(
                    protocol,
                    hostField.stringValue(),
                    portField.intValue(),
                    pathField.stringValue());
            if(protocol.getType() == Protocol.Type.ftp) {
                if(connectmodePopup.titleOfSelectedItem().equals(DEFAULT)) {
                    host.setFTPConnectMode(null);
                }
                else if(connectmodePopup.titleOfSelectedItem().equals(CONNECTMODE_ACTIVE)) {
                    host.setFTPConnectMode(FTPConnectMode.PORT);
                }
                else if(connectmodePopup.titleOfSelectedItem().equals(CONNECTMODE_PASSIVE)) {
                    host.setFTPConnectMode(FTPConnectMode.PASV);
                }
            }
            final Credentials credentials = host.getCredentials();
            credentials.setUsername(usernameField.stringValue());
            credentials.setPassword(passField.stringValue());
            credentials.setSaved(keychainCheckbox.state() == NSCell.NSOnState);
            if(protocol.getScheme().equals(Scheme.sftp)) {
                if(pkCheckbox.state() == NSCell.NSOnState) {
                    credentials.setIdentity(LocalFactory.createLocal(pkLabel.stringValue()));
                }
            }
            if(encodingPopup.titleOfSelectedItem().equals(DEFAULT)) {
                host.setEncoding(null);
            }
            else {
                host.setEncoding(encodingPopup.titleOfSelectedItem());
            }
            ((BrowserController) parent).mount(host);
        }
        Preferences.instance().setProperty("connection.toggle.options", this.toggleOptionsButton.state());
    }
}
