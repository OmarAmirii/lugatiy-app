package nl.lugatiy.api;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/** Checks top-level ISO BMFF container structure without loading video data into memory. */
final class Mp4Validator {
 private Mp4Validator() {}
 static boolean valid(Path path) throws IOException {
  try(FileChannel channel=FileChannel.open(path,StandardOpenOption.READ)) {
   long length=channel.size(),offset=0; boolean ftyp=false,moov=false,mdat=false; int boxes=0;
   while(offset<length && ++boxes<=100000) {
    if(length-offset<8)return false;
    ByteBuffer header=ByteBuffer.allocate(16);channel.position(offset);header.limit(8);if(channel.read(header)!=8)return false;header.flip();long size=Integer.toUnsignedLong(header.getInt());byte[] typeBytes=new byte[4];header.get(typeBytes);String type=new String(typeBytes,StandardCharsets.US_ASCII);int headerSize=8;
    if(size==1){header.clear();header.limit(8);if(channel.read(header)!=8)return false;header.flip();size=header.getLong();headerSize=16;}
    else if(size==0)size=length-offset;
    if(size<headerSize||size>length-offset)return false;
    if(offset==0&&!type.equals("ftyp"))return false;
    if(type.equals("ftyp"))ftyp=size>=16;
    if(type.equals("moov"))moov=size>8;
    if(type.equals("mdat"))mdat=size>8;
    offset+=size;
   }
   return offset==length&&ftyp&&moov&&mdat;
  }
 }
}
