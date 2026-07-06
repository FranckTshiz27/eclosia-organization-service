package eclosia.eclosia_organization_service.report.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FrenchAmountInWordsConverter {

    private static final String[] UNITS = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"
    };

    private FrenchAmountInWordsConverter() {
    }

    public static String convert(BigDecimal amount, String currencyName) {
        BigDecimal normalized = amount != null
                ? amount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        long integerPart = normalized.longValue();
        int cents = normalized.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        StringBuilder result = new StringBuilder(convertInteger(integerPart));
        result.append(' ').append(currencyName != null && !currencyName.isBlank() ? currencyName : "monnaie locale");

        if (cents > 0) {
            result.append(" et ").append(convertInteger(cents));
            result.append(cents > 1 ? " centimes" : " centime");
        }

        return capitalize(result.toString());
    }

    private static String convertInteger(long number) {
        if (number == 0) {
            return UNITS[0];
        }
        if (number < 0) {
            return "moins " + convertInteger(-number);
        }

        StringBuilder words = new StringBuilder();

        long billions = number / 1_000_000_000L;
        number %= 1_000_000_000L;
        if (billions > 0) {
            words.append(convertBelowThousand((int) billions)).append(" milliard");
            if (billions > 1) {
                words.append('s');
            }
        }

        long millions = number / 1_000_000L;
        number %= 1_000_000L;
        if (millions > 0) {
            appendSegment(words, convertBelowThousand((int) millions) + " million" + (millions > 1 ? "s" : ""));
        }

        long thousands = number / 1_000L;
        number %= 1_000L;
        if (thousands > 0) {
            if (thousands == 1) {
                appendSegment(words, "mille");
            } else {
                appendSegment(words, convertBelowThousand((int) thousands) + " mille");
            }
        }

        if (number > 0) {
            appendSegment(words, convertBelowThousand((int) number));
        }

        return words.toString().trim();
    }

    private static void appendSegment(StringBuilder words, String segment) {
        if (words.length() > 0) {
            words.append(' ');
        }
        words.append(segment);
    }

    private static String convertBelowThousand(int number) {
        if (number < 20) {
            return UNITS[number];
        }

        StringBuilder words = new StringBuilder();

        int hundreds = number / 100;
        number %= 100;
        if (hundreds > 0) {
            if (hundreds == 1) {
                words.append("cent");
            } else {
                words.append(UNITS[hundreds]).append(" cent");
            }
            if (number == 0 && hundreds > 1) {
                words.append('s');
            }
        }

        if (number > 0) {
            if (words.length() > 0) {
                words.append(' ');
            }
            words.append(convertBelowHundred(number));
        }

        return words.toString();
    }

    private static String convertBelowHundred(int number) {
        if (number < 20) {
            return UNITS[number];
        }
        if (number < 70) {
            String tenWord = switch (number / 10) {
                case 2 -> "vingt";
                case 3 -> "trente";
                case 4 -> "quarante";
                case 5 -> "cinquante";
                case 6 -> "soixante";
                default -> "";
            };
            int units = number % 10;
            if (units == 0) {
                return tenWord;
            }
            if (units == 1) {
                return tenWord + " et un";
            }
            return tenWord + "-" + UNITS[units];
        }
        if (number < 80) {
            if (number == 71) {
                return "soixante et onze";
            }
            return "soixante-" + UNITS[number - 60];
        }
        if (number == 80) {
            return "quatre-vingts";
        }
        return "quatre-vingt-" + UNITS[number - 80];
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
