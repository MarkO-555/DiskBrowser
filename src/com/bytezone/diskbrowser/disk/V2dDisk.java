package com.bytezone.diskbrowser.disk;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.bytezone.diskbrowser.utilities.HexFormatter;

/*
 * Offsets in bytes
 * From To Meaning
 * 0000 0003 Length of disk image, from offset 8 to the end (big endian)
 * 0004 0007 Type indication; always 'D5NI'
 * 0008 0009 Number of tracks (typical value: $23, but can be different). Also big endian.
 * 000A 000B Track nummer * 4, starting with track # 0. Big endian.
 * 000C 000D Length of track, not counting this length-field. Big endian.
 * 000E 000E + length field - 1 Nibble-data of track, byte-for-byte.
 * After this, the pattern from offset 000A repeats for each track. Note that
 * "track number * 4" means that on a regular disk the tracks are numbered 0, 4, 8 etc.
 * Half-tracks are numbered 2, 6, etc. The stepper motor of the disk uses 4 phases to
 * travel from one track to the next, so quarter-tracks could also be stored with this
 * format (I have never heard of disk actually using quarter tracks, though).
 */

// Physical disk interleave:
// Info from http://www.applelogic.org/TheAppleIIEGettingStarted.html
// Block ..: 0 1 2 3 4 5 6 7 8 9 A B C D E F
// Position: 0 8 1 9 2 A 3 B 4 C 5 D 6 E 7 F - Prodos (.PO disks)
// Position: 0 7 E 6 D 5 C 4 B 3 A 2 9 1 8 F - Dos (.DO disks)
public class V2dDisk
{
  private static byte[] addressPrologue = { (byte) 0xD5, (byte) 0xAA, (byte) 0x96 };
  private static byte[] dataPrologue = { (byte) 0xD5, (byte) 0xAA, (byte) 0xAD };
  private static byte[] epilogue = { (byte) 0xDE, (byte) 0xAA, (byte) 0xEB };
  private static int[] interleave =
      { 0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15 };

  private final Nibblizer nibbler = new Nibblizer ();

  final File file;
  final int tracks;
  int actualTracks;

  final byte[] buffer = new byte[4096 * 35];

  public V2dDisk (File file)
  {
    this.file = file;
    int tracks = 0;
    try
    {
      byte[] diskBuffer = new byte[10];
      BufferedInputStream in = new BufferedInputStream (new FileInputStream (file));
      in.read (diskBuffer);

      int diskLength = HexFormatter.getLongBigEndian (diskBuffer, 0);   // 4 bytes
      //      System.out.printf ("Disk length: %,d%n", diskLength);
      String id = HexFormatter.getString (diskBuffer, 4, 4);            // 4 bytes
      //      System.out.printf ("ID: %s%n", id);
      tracks = HexFormatter.getShortBigEndian (diskBuffer, 8);          // 2 bytes
      //      System.out.printf ("Tracks: %d%n", tracks);

      for (int i = 0; i < tracks; i++)
      {
        byte[] trackHeader = new byte[4];
        in.read (trackHeader);
        int trackNumber = HexFormatter.getShortBigEndian (trackHeader, 0);
        int trackLength = HexFormatter.getShortBigEndian (trackHeader, 2);

        int fullTrackNo = trackNumber / 4;
        int halfTrackNo = trackNumber % 4;

        byte[] trackData = new byte[trackLength];
        in.read (trackData);

        if (processTrack (trackData, buffer))
          actualTracks++;
      }

      in.close ();
    }
    catch (IOException e)
    {
      e.printStackTrace ();
      System.exit (1);
    }

    this.tracks = tracks;
  }

  private boolean processTrack (byte[] buffer, byte[] diskBuffer)
  {
    int ptr = 0;
    int track, sector, volume, checksum;

    if (buffer[0] == (byte) 0xEB)
    {
      System.out.println ("overrun");
      ++ptr;
    }

    ptr += skipBytes (buffer, ptr, (byte) 0xFF);

    while (ptr < buffer.length)
    {
      if (matchBytes (buffer, ptr, addressPrologue)
          && matchBytes (buffer, ptr + 11, epilogue))
      {
        volume = nibbler.decode4and4 (buffer, ptr + 3);
        track = nibbler.decode4and4 (buffer, ptr + 5);
        sector = nibbler.decode4and4 (buffer, ptr + 7);
        checksum = nibbler.decode4and4 (buffer, ptr + 9);
        //        System.out.printf ("Volume: %03d, Track: %02d, Sector: %02d, Checksum: %03d%n",
        //            volume, track, sector, checksum);
        ptr += 14;
      }
      else
      {
        System.out.println ("Invalid address prologue/epilogue");
        ptr += listBytes (buffer, ptr, 14);
        return false;
      }

      ptr += skipBytes (buffer, ptr, (byte) 0xFF);

      if (!matchBytes (buffer, ptr, dataPrologue))
      {
        System.out.println ("Invalid data prologue");
        ptr += listBytes (buffer, ptr, dataPrologue.length);
        return false;
      }

      if (!matchBytes (buffer, ptr + 346, epilogue))
      {
        System.out.println ("Invalid data epilogue");
        ptr += listBytes (buffer, ptr + 346, epilogue.length);
        return false;
      }

      ptr += dataPrologue.length;

      byte[] decodedBuffer = nibbler.decode6and2 (buffer, ptr);

      int offset = track * 4096 + interleave[sector] * 256;
      System.arraycopy (decodedBuffer, 0, diskBuffer, offset, 256);

      ptr += 342;
      ptr += 1;     // checksum
      ptr += epilogue.length;

      //      System.out.print ("<--- 342 data bytes --> ");

      //      ptr += listBytes (buffer, ptr, 4);
      ptr += skipBytes (buffer, ptr, (byte) 0xFF);
    }

    //    System.out.println ("----------------------------------------------");
    return true;
  }

  private int skipBytes (byte[] buffer, int offset, byte skipValue)
  {
    int count = 0;
    while (offset < buffer.length && buffer[offset++] == skipValue)
      ++count;
    //    System.out.printf ("   (%2d x %02X)%n", count, skipValue);
    return count;
  }

  private int listBytes (byte[] buffer, int offset, int length)
  {
    int count = 0;
    for (int i = 0; i < length; i++)
    {
      if (offset >= buffer.length)
        break;
      System.out.printf ("%02X ", buffer[offset++]);
      ++count;
    }
    System.out.println ();
    return count;
  }

  private boolean matchBytes (byte[] buffer, int offset, byte[] valueBuffer)
  {
    for (int i = 0; i < valueBuffer.length; i++)
    {
      if (offset >= buffer.length)
        return false;
      if (buffer[offset++] != valueBuffer[i])
        return false;
    }
    return true;
  }
}