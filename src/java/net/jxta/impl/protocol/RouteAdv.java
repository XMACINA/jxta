/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: RouteAdv.java,v 1.1 2007/01/16 11:01:41 thomas Exp $
 */

package net.jxta.impl.protocol;


import java.net.URI;
import java.util.Enumeration;
import java.util.Vector;

import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.*;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.RouteAdvertisement;


/**
 * This class implements the basic Route advertisement.
 *
 * <pre>
 *    &lt;xs:complexType name="RA">
 *      &lt;xs:sequence>
 *        &lt;xs:element name="DstPID" type="xs:anyURI" />
 *        &lt;xs:element ref="jxta:APA" />
 *        &lt;xs:element name="Hops" minOccurs="0">
 *          &lt;xs:simpleType>
 *            &lt;xs:sequence>
 *              &lt;xs:element ref="jxta:APA" maxOccurs="unbounded" />
 *            &lt;/xs:sequence>
 *          &lt;/xs:simpleType>   
 *        &lt;/xs:element>
 *      &lt;/xs:sequence>
 *    &lt;/xs:complexType>
 *</pre>
 *
 *  @see net.jxta.protocol.RouteAdvertisement
 **/

public class RouteAdv extends RouteAdvertisement implements Cloneable {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(RouteAdv.class.getName());
    
    private static final String[] INDEX_FIELDS = { DEST_PID_TAG };
    
    /**
     * Instantiator for our advertisement
     **/
    public static class Instantiator implements AdvertisementFactory.Instantiator {
        
        /**
         * {@inheritDoc}
         **/
        public String getAdvertisementType() {
            return RouteAdv.getAdvertisementType();
        }
        
        /**
         * {@inheritDoc}
         **/
        public Advertisement newInstance() {
            return new RouteAdv();
        }
        
        /**
         * {@inheritDoc}
         **/
        public Advertisement newInstance(Element root) {
            return new RouteAdv(root);
        }
    }
    
    /**
     * Private constructor. Use instantiator
     *
     **/
    private RouteAdv() {}
    
    /**
     * Private constructor. Use instantiator
     *
     **/
    private RouteAdv(Element root) {
        if (!XMLElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports XLMElement");
        }
        
        XMLElement doc = (XMLElement) root;
        
        String doctype = doc.getName();
        
        String typedoctype = "";
        Attribute itsType = doc.getAttribute("type");

        if (null != itsType) {
            typedoctype = itsType.getValue();
        }
        
        if (!doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype)) {
            throw new IllegalArgumentException("Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }
        
        Enumeration elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();
            
            if (!handleElement(elem)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Unhandled Element: " + elem.toString());
                }
            }
        }
        
        // HACK Compatibility 
        
        setDestPeerID(getDestPeerID());
        
        // Sanity Check!!!
        
        if (hasALoop()) {
            throw new IllegalArgumentException("Route contains a loop!");
        }
    }
    
    /**
     *  {@inheritDoc}
     */
    public Object clone() {
        RouteAdv a = (RouteAdv) super.clone();

        a.setDest( (AccessPointAdvertisement) getDest().clone() );
        
        // deep copy of the hops
        Vector clonehops = getVectorHops();
        int size = clonehops.size();
        for (int i = 0; i < size; ++i) {
            clonehops.set( i, ((AccessPointAdvertisement) clonehops.get(i)).clone() );
        }
        
        a.setHops( clonehops );
        
        return a;
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected boolean handleElement(Element raw) {
        
        if (super.handleElement(raw)) {
            return true;
        }
        
        XMLElement elem = (XMLElement) raw;
        
        if (DEST_PID_TAG.equals(elem.getName())) {
            try {
                URI pID = new URI (elem.getTextValue());

                setDestPeerID((PeerID) IDFactory.fromURI(pID));
            } catch (URISyntaxException badID) {
                throw new IllegalArgumentException("Bad PeerID in advertisement");
            } catch (ClassCastException badID) {
                throw new IllegalArgumentException("ID in advertisement is not a peer id");
            }
            return true;
        }
        
        if (elem.getName().equals("Dst")) {
            for (Enumeration eachXpt = elem.getChildren(); eachXpt.hasMoreElements();) {
                TextElement aXpt = (TextElement) eachXpt.nextElement();
                
                AccessPointAdvertisement xptAdv = (AccessPointAdvertisement)
                        AdvertisementFactory.newAdvertisement(aXpt);

                setDest(xptAdv);
            }
            return true;
        }
        
        if (elem.getName().equals("Hops")) {
            Vector hops = new Vector();

            for (Enumeration eachXpt = elem.getChildren(); eachXpt.hasMoreElements();) {
                TextElement aXpt = (TextElement) eachXpt.nextElement();
                
                AccessPointAdvertisement xptAdv = (AccessPointAdvertisement)
                        AdvertisementFactory.newAdvertisement(aXpt);

                hops.addElement(xptAdv);
            }
            setHops(hops);
            return true;
        }
        
        return false;
        
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Document getDocument(MimeMediaType encodeAs) {
        StructuredDocument adv = (StructuredDocument) super.getDocument(encodeAs);
        
        if (hasALoop()) {
            throw new IllegalStateException("I won't write a doc for a route with a loop");
        }
        
        PeerID pid = getDestPeerID();
        AccessPointAdvertisement dest = getDest();
        
        if ((null != pid) && (null != dest) && (null != dest.getPeerID())) {
            if (!pid.equals(dest.getPeerID())) {
                throw new IllegalStateException("Destination peer id and destination access point adv don't refer to the same peer");
            }
        }
        
        // HACK Backwards Compatibility
        if ((null == pid) && (null != dest)) {
            pid = dest.getPeerID();
        }
        
        if (pid != null) {
            Element e0 = adv.createElement(DEST_PID_TAG, pid.toString());

            adv.appendChild(e0);
        }
        
        if (dest != null) {
            // create the copy without the PID
            AccessPointAdvertisement ap = (AccessPointAdvertisement) AdvertisementFactory.newAdvertisement(
                    AccessPointAdvertisement.getAdvertisementType());
            
            ap.setEndpointAddresses(dest.getVectorEndpointAddresses());
            
            StructuredTextDocument xptDoc = (StructuredTextDocument) ap.getDocument(encodeAs);
            
            Element e1 = adv.createElement("Dst");

            adv.appendChild(e1);
            StructuredDocumentUtils.copyElements(adv, e1, xptDoc);
        }
        
        // only include hops if we have some
        if (getHops().hasMoreElements()) {
            
            Element e2 = adv.createElement("Hops");

            adv.appendChild(e2);
            
            for (Enumeration e = getHops(); e.hasMoreElements();) {
                AccessPointAdvertisement hop = (AccessPointAdvertisement) e.nextElement();

                if (hop != null) {
                    StructuredTextDocument xptDoc = (StructuredTextDocument)
                            hop.getDocument(encodeAs);
                    
                    StructuredDocumentUtils.copyElements(adv, e2, xptDoc);
                }
            }
        }
        
        return adv;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String[] getIndexFields() {
        return INDEX_FIELDS;
    }
}
