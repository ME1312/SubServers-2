package net.ME1312.SubServers.Client.Bukkit.Library.Version;

final class VersionTokenizer {
 private final String _versionString;
 private final int _length;

 private int _position;
 private int _number;
 private String _suffix;
 private boolean _hasValue;

 protected int getNumber() {
     return _number;
 }

 protected String getSuffix() {
     return _suffix;
 }

 protected boolean hasValue() {
     return _hasValue;
 }

 protected VersionTokenizer(String versionString) {
     if (versionString == null)
         throw new IllegalArgumentException("versionString is null");

     _versionString = versionString;
     _length = versionString.length();
 }

 protected boolean MoveNext() {
     _number = 0;
     _suffix = "";
     _hasValue = false;

     // No more characters
     if (_position >= _length)
         return false;

     _hasValue = true;

     while (_position < _length) {
         char c = _versionString.charAt(_position);
         if (c < '0' || c > '9') break;
         _number = _number * 10 + (c - '0');
         _position++;
     }

     int suffixStart = _position;

     while (_position < _length) {
         char c = _versionString.charAt(_position);
         if (c == '.') break;
         _position++;
     }

     _suffix = _versionString.substring(suffixStart, _position);

     if (_position < _length) _position++;

     return true;
 }
}