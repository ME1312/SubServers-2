package net.ME1312.SubServers.Bungee.Library.Version;

import java.io.Serializable;

/**
 * Version Class
 */
@SuppressWarnings("serial")
public class Version implements Serializable, Comparable<Version> {
	private String string;

    /**
     * Creates a Version
     *
     * @param string Version String
     */
	public Version(String string) {
		this.string = string;
	}


    /**
     * Creates a Version
     *
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(Integer... ints) {
        String string = Integer.toString(ints[0]);
        int i = 0;
        if (ints.length != 1) {
            do {
                i++;
                string = string + "." + ints[i];
            } while ((i + 1) != ints.length);
        }
        this.string = string;
    }
	
	@Override
	public String toString() {
		return string;
	}

    /**
     * See if Versions are Equal
     *
     * @param version Version to Compare
     * @return
     */
    public boolean equals(Version version) {
        return compareTo(version) == 0;
    }
    
    /*
     * Returns 1 if Greater than
     * Returns 0 if Equal
     * Returns -1 if Less than 
     *//**
     *
     * Compare Versions
     *
     * @param version The version to compare to
     */
    public int compareTo(Version version) {
        String version1 = this.string;
        String version2 = version.toString();

        VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
        VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

        int number1 = 0, number2 = 0;
        String suffix1 = "", suffix2 = "";

        while (tokenizer1.MoveNext()) {
            if (!tokenizer2.MoveNext()) {
                do {
                    number1 = tokenizer1.getNumber();
                    suffix1 = tokenizer1.getSuffix();
                    if (number1 != 0 || suffix1.length() != 0) {
                        // Version one is longer than number two, and non-zero
                        return 1;
                    }
                }
                while (tokenizer1.MoveNext());

                // Version one is longer than version two, but zero
                return 0;
            }

            number1 = tokenizer1.getNumber();
            suffix1 = tokenizer1.getSuffix();
            number2 = tokenizer2.getNumber();
            suffix2 = tokenizer2.getSuffix();

            if (number1 < number2) {
                // Number one is less than number two
                return -1;
            }
            if (number1 > number2) {
                // Number one is greater than number two
                return 1;
            }

            boolean empty1 = suffix1.length() == 0;
            boolean empty2 = suffix2.length() == 0;

            if (empty1 && empty2) continue; // No suffixes
            if (empty1) return 1; // First suffix is empty (1.2 > 1.2b)
            if (empty2) return -1; // Second suffix is empty (1.2a < 1.2)

            // Lexical comparison of suffixes
            int result = suffix1.compareTo(suffix2);
            if (result != 0) return result;

        }
        if (tokenizer2.MoveNext()) {
            do {
                number2 = tokenizer2.getNumber();
                suffix2 = tokenizer2.getSuffix();
                if (number2 != 0 || suffix2.length() != 0) {
                    // Version one is longer than version two, and non-zero
                    return -1;
                }
            }
            while (tokenizer2.MoveNext());

            // Version two is longer than version one, but zero
            return 0;
        }
        return 0;
    }

    /**
     * See if Versions are Equal
     *
     * @param ver1 Version to Compare
     * @param ver2 Version to Compare
     * @return
     */
    public static boolean isEqual(Version ver1, Version ver2) {
        return compare(ver1, ver2) == 0;
    }

    /*
     * Returns 1 if Greater than
     * Returns 0 if Equal
     * Returns -1 if Less than
     *//**
     * Compare Versions
     *
     * @param ver1 Version to Compare
     * @param ver2 Version to Compare
     */
    public static int compare(Version ver1, Version ver2) {
        String version1 = ver1.toString();
        String version2 = ver2.toString();

        VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
        VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

        int number1 = 0, number2 = 0;
        String suffix1 = "", suffix2 = "";

        while (tokenizer1.MoveNext()) {
            if (!tokenizer2.MoveNext()) {
                do {
                    number1 = tokenizer1.getNumber();
                    suffix1 = tokenizer1.getSuffix();
                    if (number1 != 0 || suffix1.length() != 0) {
                        // Version one is longer than number two, and non-zero
                        return 1;
                    }
                }
                while (tokenizer1.MoveNext());

                // Version one is longer than version two, but zero
                return 0;
            }

            number1 = tokenizer1.getNumber();
            suffix1 = tokenizer1.getSuffix();
            number2 = tokenizer2.getNumber();
            suffix2 = tokenizer2.getSuffix();

            if (number1 < number2) {
                // Number one is less than number two
                return -1;
            }
            if (number1 > number2) {
                // Number one is greater than number two
                return 1;
            }

            boolean empty1 = suffix1.length() == 0;
            boolean empty2 = suffix2.length() == 0;

            if (empty1 && empty2) continue; // No suffixes
            if (empty1) return 1; // First suffix is empty (1.2 > 1.2b)
            if (empty2) return -1; // Second suffix is empty (1.2a < 1.2)

            // Lexical comparison of suffixes
            int result = suffix1.compareTo(suffix2);
            if (result != 0) return result;

        }
        if (tokenizer2.MoveNext()) {
            do {
                number2 = tokenizer2.getNumber();
                suffix2 = tokenizer2.getSuffix();
                if (number2 != 0 || suffix2.length() != 0) {
                    // Version one is longer than version two, and non-zero
                    return -1;
                }
            }
            while (tokenizer2.MoveNext());

            // Version two is longer than version one, but zero
            return 0;
        }
        return 0;
    }
}