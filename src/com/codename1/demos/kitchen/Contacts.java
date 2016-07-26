/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.demos.kitchen;

import com.codename1.components.InfiniteProgress;
import com.codename1.components.MultiButton;
import com.codename1.components.ShareButton;
import com.codename1.components.ToastBar;
import com.codename1.contacts.Contact;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.messaging.Message;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.SwipeableContainer;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.table.TableLayout;
import com.codename1.util.CaseInsensitiveOrder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Shai Almog
 */
public class Contacts extends Demo {    
    private HashMap<String, Image> letterCache = new HashMap<>();
    private Image circleLineImage;
    private Object circleMask;
    private int circleMaskWidth;
    private int circleMaskHeight;
    private Font letterFont;
    private boolean finishedLoading;
    private Label scrollLetter;
    private int screenY;
    
    public String getDisplayName() {
        return "Contacts";
    }

    public Image getDemoIcon() {
        return getResources().getImage("contacts.png");
    }
    
    public Image getLetter(char c, Component cmp) {
        c = Character.toUpperCase(c);
        String cstr = "" + c;
        Image i = letterCache.get(cstr);
        if(i != null) {
            return i;
        }
         
        int off = (c - 'A') % 7 + 1;
        int color = cmp.getUIManager().getComponentStyle("Blank" + off).getBgColor();
        Image img = Image.createImage(circleMaskWidth, circleMaskHeight, 0);
        Graphics g = img.getGraphics();
        g.setColor(color);
        g.fillArc(1, 1, circleMaskWidth - 2, circleMaskHeight - 2, 0, 360);
        g.setFont(letterFont);
        g.setColor(0xffffff);
        int w = letterFont.charWidth(c);
        g.drawString(cstr, img.getWidth() / 2 - w / 2, img.getHeight() / 2 - letterFont.getHeight() / 2);
        g.drawImage(circleLineImage, 0, 0);
        letterCache.put(cstr, img);
        return img;
    }
    
    private String notNullOrEmpty(String... s) {
        StringBuilder b = new StringBuilder();
        for(String ss : s) {
            if(ss == null || ss.length() == 0) {
                return "";
            }
            b.append(ss);
        }
        return b.toString();
    }
    
    public Container createDemo(Form parentForm) {
        Image circleImage = getResources().getImage("circle.png");
        circleLineImage = getResources().getImage("circle-line.png");
        
        circleMask = circleImage.createMask();
        circleMaskWidth = circleImage.getWidth();
        circleMaskHeight = circleImage.getHeight();
        letterFont = Font.createTrueTypeFont("native:MainThin", "native:MainThin");
        letterFont = letterFont.derive(circleMaskHeight - circleMaskHeight/ 3, Font.STYLE_PLAIN);
        
        parentForm.addPointerReleasedListener(e -> {
            if(scrollLetter != null) {
                parentForm.getLayeredPane().removeAll();
                scrollLetter = null;
            }
        });
        parentForm.addPointerDraggedListener(e -> {
            screenY = e.getY();
        });
        
        final Container scrollbarParent = new Container(new BorderLayout());
        final Container contactsDemo = new Container(BoxLayout.y());        
        contactsDemo.setScrollableY(true);
        contactsDemo.add(FlowLayout.encloseCenterMiddle(new InfiniteProgress()));
        
        
        contactsDemo.setScrollVisible(false);
        Slider scroll = new Slider();
        scroll.setUIID("Container");
        scroll.setThumbImage(Image.createImage(5,40, 0xff000000));
        scroll.setVertical(true);
        scroll.setMinValue(0);
        scroll.setEditable(true);
        scrollbarParent.add(BorderLayout.EAST, scroll);
        scrollbarParent.add(BorderLayout.CENTER, contactsDemo);
        
        final boolean[] lock = new boolean[1];
        scroll.addDataChangedListener((type, index) -> {
            if(!lock[0]) {
                lock[0] = true;
                if(scrollLetter == null) {
                    scrollLetter = new Label();
                    Style ss = scrollLetter.getUIManager().getComponentStyle("Blank5");
                    ss.setFgColor(ss.getBgColor());
                    ss.setBgTransparency(0);
                    Style us = scrollLetter.getUnselectedStyle();
                    us.setBgImage(FontImage.createMaterial(FontImage.MATERIAL_LABEL, 
                            ss, 10));
                    us.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FIT);
                    us.setFgColor(0xffffff);
                    us.setMarginUnit(Style.UNIT_TYPE_PIXELS);
                    us.setPaddingUnit(Style.UNIT_TYPE_DIPS);
                    us.setFont(us.getFont().derive(Display.getInstance().convertToPixels(6), Font.STYLE_PLAIN));
                    us.setPadding(4, 4, 4, 4);
                    Container cnt = parentForm.getLayeredPane();
                    cnt.setLayout(new BorderLayout());
                    cnt.add(BorderLayout.EAST, scrollLetter);
                    scrollLetter.getUnselectedStyle().setMargin(Component.RIGHT, scroll.getWidth());
                }
                scrollLetter.getUnselectedStyle().setMargin(Component.TOP, Math.max(0, screenY - parentForm.getContentPane().getY() - contactsDemo.getScrollY()));
                contactsDemo.scrollRectToVisible(0, scroll.getMaxValue() - index, 5, contactsDemo.getHeight(), contactsDemo);
                scroll.setMaxValue(contactsDemo.getScrollDimension().getHeight());
                Component cmp = contactsDemo.getComponentAt(contactsDemo.getWidth() / 2, screenY);
                if(cmp == null) {
                    scrollLetter.setText("");
                    lock[0] = false;
                    return;
                }
                while(!(cmp instanceof MultiButton)) {
                    cmp = cmp.getParent();
                    if(cmp == null) {
                        scrollLetter.setText("");
                        lock[0] = false;
                        return;
                    }
                }
                if(cmp instanceof MultiButton) {
                    String s = (String)cmp.getClientProperty("char");
                    if(!s.equals(scrollLetter.getText())) {
                        scrollLetter.setText(s);
                        scrollLetter.getParent().revalidate();
                    }
                }
                lock[0] = false;
            }
        });
        contactsDemo.addScrollListener((scrollX, scrollY, oldscrollX, oldscrollY) -> {
            if(!lock[0]) {
                lock[0] = true;
                scroll.setProgress(scroll.getMaxValue() - scrollY);
                scroll.setMaxValue(contactsDemo.getScrollDimension().getHeight());
                lock[0] = false;
            }
        });        
        
        Display.getInstance().scheduleBackgroundTask(() -> {
            Contact[] contacts = Display.getInstance().getAllContacts(true, true, false, true, true, false);
            CaseInsensitiveOrder co = new CaseInsensitiveOrder();
            Arrays.sort(contacts, (o1, o2) -> {
                String sname1  = o1.getFamilyName();
                String sname2  = o2.getFamilyName();
                if(sname1 == null) {
                    sname1 = o1.getDisplayName();
                    if(sname1 == null) {
                        sname1 = "";
                    }
                }
                if(sname2 == null) {
                    sname2 = o2.getDisplayName();
                    if(sname2 == null) {
                        sname2 = "";
                    }
                }
                return co.compare(sname1, sname2);
            });
            Display.getInstance().callSerially(() -> {
                contactsDemo.removeAll();
                for(Contact c : contacts) {
                    String dname = c.getDisplayName();
                    if(dname == null || dname.length() == 0) {
                        continue;
                    }
                    MultiButton mb = new MultiButton(dname);
                    mb.setIconUIID("ContactIcon");
                    String fname = c.getFamilyName();
                    if(fname != null && fname.length() > 0) {
                        mb.putClientProperty("char", "" + fname.charAt(0));
                    } else {
                        mb.putClientProperty("char", "" + dname.charAt(0));                        
                    }
                    
                    // we need this for the SwipableContainer below
                    mb.getAllStyles().setBgTransparency(255);
                    mb.setTextLine2(c.getNote());
                    mb.setIcon(getLetter(dname.charAt(0), mb));
                    Button delete = new Button();
                    delete.setUIID("SwipeableContainerButton");
                    FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE, 8);
                    
                    Button info = new Button();
                    info.setUIID("SwipeableContainerInfoButton");
                    FontImage.setMaterialIcon(info, FontImage.MATERIAL_INFO, 8);
                    info.addActionListener(e -> {
                        Dialog dlg = new Dialog(dname);
                        TableLayout tl = new TableLayout(3, 2);
                        dlg.setLayout(tl);
                        Map emailHash = c.getEmails();
                        Container emails;
                        if(emailHash != null && emailHash.size() > 0) {
                            Button[] emailArr = new Button[emailHash.size()];
                            int off = 0;
                            for(Object ee : emailHash.values()) {
                                emailArr[off] = new Button((String)ee);
                                FontImage.setMaterialIcon(emailArr[off], FontImage.MATERIAL_EMAIL);
                                emailArr[off].addActionListener(ev -> {
                                    dlg.dispose();
                                    Message m = new Message("");
                                    Display.getInstance().sendMessage(new String[] {(String)ee}, "Sent from Codename One!", m);
                                });
                                off ++;
                            }
                            emails = BoxLayout.encloseY(emailArr);
                        } else {
                            emails = new Container(BoxLayout.y());
                        }

                        Map phonesHash = c.getPhoneNumbers();
                        Container phones;
                        if(phonesHash != null && phonesHash.size() > 0) {
                            Button[] phoneArr = new Button[phonesHash.size()];
                            int off = 0;
                            for(Object ee : phonesHash.values()) {
                                phoneArr[off] = new Button((String)ee);
                                FontImage.setMaterialIcon(phoneArr[off], FontImage.MATERIAL_PHONE);
                                phoneArr[off].addActionListener(ev -> {
                                    dlg.dispose();
                                    Display.getInstance().dial((String)ee);
                                });
                                off ++;
                            }
                            phones = BoxLayout.encloseY(phoneArr);
                        } else {
                            phones = new Container(BoxLayout.y());
                        }
                        
                        
                        dlg.add("Phones").add(phones).
                                add("Emails").add(emails);
                        
                        if(c.getBirthday() == 0) {
                            dlg.add("Birthday").add("---");
                        } else {
                            dlg.add("Birthday").add(L10NManager.getInstance().formatDateShortStyle(new Date(c.getBirthday())));
                        }
                        dlg.setDisposeWhenPointerOutOfBounds(true);
                        dlg.setBackCommand(new Command(""));
                        dlg.showPacked(BorderLayout.SOUTH, true);
                    });

                    ShareButton share = new ShareButton();
                    share.setUIID("SwipeableContainerShareButton");
                    FontImage.setMaterialIcon(share, FontImage.MATERIAL_SHARE, 8);
                    share.setText("");
                    share.setTextToShare(dname + notNullOrEmpty(" phone: ", c.getPrimaryPhoneNumber()) + 
                            notNullOrEmpty(" email: ", c.getPrimaryEmail()));

                    Button call = new Button();
                    call.setUIID("SwipeableContainerInfoButton");
                    FontImage.setMaterialIcon(call, FontImage.MATERIAL_CALL, 8);
                    call.addActionListener(e -> Display.getInstance().dial(c.getPrimaryPhoneNumber()));

                    Container options;
                    if(c.getPrimaryEmail() != null && c.getPrimaryEmail().length() > 0) {
                        Button email = new Button();
                        email.setUIID("SwipeableContainerInfoButton");
                        FontImage.setMaterialIcon(email, FontImage.MATERIAL_EMAIL, 8);
                        email.addActionListener(e -> {
                            Message m = new Message("");
                            Display.getInstance().sendMessage(new String[] {c.getPrimaryEmail()}, "Sent from Codename One!", m);
                        });
                        options = GridLayout.encloseIn(4, call, email, info, share);
                    }  else {
                        options = GridLayout.encloseIn(3, call, info, share);
                    }
                    
                    
                    SwipeableContainer sc = new SwipeableContainer(
                            options, 
                            GridLayout.encloseIn(1, delete), 
                            mb);
                    contactsDemo.add(sc);
                    sc.addSwipeOpenListener(e -> {
                        // auto fold the swipe when we go back to scrolling
                        contactsDemo.addScrollListener(new ScrollListener() {
                            int initial = -1;
                            @Override
                            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                                // scrolling is very sensitive on devices...
                                if(initial < 0) {
                                    initial = scrollY;
                                }
                                if(Math.abs(scrollY - initial) > mb.getHeight() / 2) {
                                    if(sc.getParent() != null) {
                                        sc.close();
                                    }
                                    contactsDemo.removeScrollListener(this);
                                }
                            }
                        });
                    });
                                        
                    delete.addActionListener(e -> {
                        if(Dialog.show("Delete", "Are you sure?\nThis will delete this contact permanently!", "Delete", "Cancel")) {
                            Display.getInstance().deleteContact(c.getId());
                            sc.remove();
                            contactsDemo.animateLayout(800);
                        }
                    });
                    
                    Display.getInstance().scheduleBackgroundTask(() -> {
                        // let the UI finish loading first before we proceed with the images
                        while(!finishedLoading) {
                            Util.sleep(100);
                        }
                        
                        // fetch only the picture which is the last missing piece
                        Contact picContact = Display.getInstance().getContactById(c.getId(), false, true, false, false, false);
                        Image img = picContact.getPhoto();
                        if(img != null) {
                            // UI/Image manipulation must be done on the EDT
                            Display.getInstance().callSerially(() -> {
                                Image rounded = img.fill(circleMaskWidth, circleMaskHeight).applyMask(circleMask);
                                Image mutable = Image.createImage(circleMaskWidth, circleMaskHeight, 0);
                                Graphics g = mutable.getGraphics();
                                g.drawImage(rounded, 0, 0);
                                g.drawImage(circleLineImage, 0, 0);
                                mb.setIcon(mutable);
                                mb.getIconComponent().repaint();
                            });
                            
                            // yield slightly so we don't choke the EDT while a user might be scrolling...
                            Util.sleep(5);
                        }
                    });
                }
                contactsDemo.revalidate();
                scroll.setMaxValue(contactsDemo.getScrollDimension().getHeight());
                scroll.setProgress(scroll.getMaxValue());
                                
                finishedLoading = true;
                ToastBar.showMessage("Swipe the contacts to both sides to expose additional options", FontImage.MATERIAL_COMPARE_ARROWS, 5000);
            });
        });
                
        return scrollbarParent;
    }
    
}
