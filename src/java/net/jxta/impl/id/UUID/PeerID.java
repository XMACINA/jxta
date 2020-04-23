/*
 *
 * $Id: PeerID.java,v 1.1 2007/01/16 11:02:08 thomas Exp $
 *
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.impl.id.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 *  An implementation of the {@link net.jxta.peer.PeerID} ID Type.
 **/
public class PeerID extends net.jxta.peer.PeerID {
    
    /**
     *  Log4J Logger
     **/
    private static final transient Logger LOG = Logger.getLogger( PeerID.class.getName() );
    
    protected final static int groupIdOffset = 0;
    protected final static int idOffset = PeerID.groupIdOffset + IDFormat.uuidSize;
    protected final static int padOffset = PeerID.idOffset + IDFormat.uuidSize;
    
    protected final static int padSize = IDFormat.flagsOffset - PeerID.padOffset;
    
    /**
     *  The id data
     **/
    protected IDBytes id;
    
    /**
     *  Used only internally.
     **/
    protected PeerID() {
        super();
        id = new IDBytes();
        id.bytes[IDFormat.flagsOffset + IDFormat.flagsIdTypeOffset] = IDFormat.flagPeerID;
    }
    
    /**
     * Intializes contents from provided ID.
     *
     * @param id    the ID data
     **/
    protected PeerID( IDBytes id ) {
        super();
        this.id = id;
    }
    
    /**
     * Creates a PeerID. A PeerGroupID is provided
     *
     * @param groupUUID    the UUID of the group to which this will belong.
     * @param idUUID    the UUID which will be used for this pipe.
     **/
    protected PeerID( UUID groupUUID,  UUID idUUID ) {
        this( );
        
        id.longIntoBytes(
                PipeID.groupIdOffset, groupUUID.getMostSignificantBits() );
        id.longIntoBytes(
                PipeID.groupIdOffset + 8, groupUUID.getLeastSignificantBits() );
        
        id.longIntoBytes(
                PipeID.idOffset, idUUID.getMostSignificantBits() );
        id.longIntoBytes(
                PipeID.idOffset + 8, idUUID.getLeastSignificantBits() );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newPeerID(net.jxta.peergroup.PeerGroupID)}.
     **/
    public PeerID(PeerGroupID groupID) {
        this( groupID.getUUID(), UUIDFactory.newUUID() );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newPeerID(net.jxta.peergroup.PeerGroupID,byte[])}.
     **/
    public PeerID( PeerGroupID groupID, byte [] seed ) {
        this( );
        
        UUID groupUUID = new UUID(
                groupID.id.bytesIntoLong( PeerGroupID.groupIdOffset ),
                groupID.id.bytesIntoLong( PeerGroupID.groupIdOffset + 8 ) );
        
        byte [] idUUIDbytes = new byte [IDFormat.uuidSize];
        
        System.arraycopy( seed, 0, idUUIDbytes, 0, Math.min( IDFormat.uuidSize, seed.length ) );
        
        UUID idUUID = UUIDFactory.newUUID( idUUIDbytes );
        
        id.longIntoBytes(
                PipeID.groupIdOffset, groupUUID.getMostSignificantBits() );
        id.longIntoBytes(
                PipeID.groupIdOffset + 8, groupUUID.getLeastSignificantBits() );
        
        id.longIntoBytes(
                PipeID.idOffset, idUUID.getMostSignificantBits() );
        id.longIntoBytes(
                PipeID.idOffset + 8, idUUID.getLeastSignificantBits() );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        if (this == target) {
            return true;
        }
        
        if (target instanceof PeerID ) {
            PeerID peerTarget = (PeerID)target;
            
            if( !getIDFormat().equals( peerTarget.getIDFormat() ) ) {
                return false;
            }
            
            if( id == peerTarget.id ) {
                return true;
            }
            
            boolean result = id.equals( peerTarget.id );
            
            // if true then we can have the two ids share the id bytes
            if( result ) {
                peerTarget.id = id;
            }
            
            return result;
        } else {
            return false;
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getIDFormat() {
        return IDFormat.INSTANTIATOR.getSupportedIDFormat();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Object getUniqueValue() {
        return getIDFormat() + "-" + (String) id.getUniqueValue();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public net.jxta.id.ID getPeerGroupID() {
        UUID groupUUID = new UUID(
                id.bytesIntoLong( PeerID.groupIdOffset ),
                id.bytesIntoLong( PeerID.groupIdOffset + 8 ) );
        
        PeerGroupID groupID = new PeerGroupID( groupUUID );
        
        // convert to the generic world PGID as necessary
        return IDFormat.translateToWellKnown( groupID );
    }
    
}
