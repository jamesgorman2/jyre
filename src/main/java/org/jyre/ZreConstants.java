package org.jyre;

interface ZreConstants {
    // ZRE-DISC port assigned by IANA
    int PING_PORT = 5670;

    // Intervals to be configured/reviewed
    int PING_INTERVAL   = 1000;   // Once per second
    int PEER_EVASIVE    = 5000;   // Five seconds - silence is evasive
    int PEER_EXPIRED   = 10000;   // Ten seconds - silence is expired
    int PEER_HWM       =  1000;   // Approximately 100 messages per second

    // Agent constants
    int USHORT_MAX = 0xffff;
    int UBYTE_MAX = 0xff;
}
