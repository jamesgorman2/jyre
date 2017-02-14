package org.jyre;

interface ZreConstants {
    // Defined port numbers, pending IANA submission
    int PING_PORT = 9991;
    int LOG_PORT  = 9992;

    // Intervals to be configured/reviewed
    int PING_INTERVAL   = 1000;   //  Once per second
    int PEER_EVASIVE    = 5000;   //  Five seconds - silence is evasive
    int PEER_EXPIRED   = 10000;   //  Ten seconds - silence is expired

    // Agent constants
    int USHORT_MAX = 0xffff;
    int UBYTE_MAX = 0xff;
}
