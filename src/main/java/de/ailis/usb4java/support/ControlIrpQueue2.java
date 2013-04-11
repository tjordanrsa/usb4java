/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.txt for licensing information.
 */

package de.ailis.usb4java.support;

import java.nio.ByteBuffer;

import javax.usb.UsbControlIrp;
import javax.usb.UsbException;
import javax.usb.UsbIrp;
import javax.usb.UsbShortPacketException;
import javax.usb.event.UsbDeviceDataEvent;

import de.ailis.usb4java.exceptions.Usb4JavaException;
import de.ailis.usb4java.libusb.DeviceHandle;
import de.ailis.usb4java.libusb.LibUSB;
import de.ailis.usb4java.topology.Usb4JavaDevice;

/**
 * A queue for USB control I/O request packets.
 *
 * @author Klaus Reimer (k@ailis.de)
 */
public final class ControlIrpQueue2 extends AbstractIrpQueue2<UsbControlIrp>
{
    /** The USB device listener list. */
    private final UsbDeviceListenerList listeners;
    
    /**
     * Constructor.
     * 
     * @param device
     *            The USB device.
     * @param listeners
     *            The USB device listener list.
     */
    public ControlIrpQueue2(final Usb4JavaDevice device, 
        final UsbDeviceListenerList listeners)
    {
        super(device);
        this.listeners = listeners;
    }

    /**
     * @see AbstractIrpQueue2#processIrp(javax.usb.UsbIrp)
     */
    @Override
    protected void processIrp(final UsbControlIrp irp) throws UsbException
    {
        final ByteBuffer buffer =
            ByteBuffer.allocateDirect(irp.getLength());
        buffer.put(irp.getData(), irp.getOffset(), irp.getLength());
        buffer.rewind();
        final DeviceHandle handle = this.device.open();
        final int len = LibUSB.controlTransfer(handle, irp.bmRequestType(),
            irp.bRequest(), irp.wValue(), irp.wIndex(), buffer,
            getConfig().getTimeout());
        if (len < 0)
            throw new Usb4JavaException(
                "Unable to submit control message", len);
        buffer.rewind();
        buffer.get(irp.getData(), irp.getOffset(), len);
        irp.setActualLength(len);
        if (irp.getActualLength() != irp.getLength() && !irp.getAcceptShortPacket())
            throw new UsbShortPacketException();
    }

    /**
     * @see AbstractIrpQueue2#finishIrp(javax.usb.UsbIrp)
     */
    @Override
    protected void finishIrp(final UsbIrp irp)
    {
        this.listeners.dataEventOccurred(new UsbDeviceDataEvent( 
            this.device, (UsbControlIrp) irp));
    }
}