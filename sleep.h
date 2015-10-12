#define adc_disable()  (ADCSRA &= ~(1<<ADEN)) // disable ADC (before power-off)
#define adc_enable()   (ADCSRA |=  (1<<ADEN)) // re-enable ADC
#define ac_disable()   (ACSR   |=  (1<<ACD )) // disable analogue comparator
#define ac_enable()    (ACSR   &= ~(1<<ACD )) // enable analogue comparator
#ifndef sleep_bod_disable() // not included in Arduino AVR toolset
#define sleep_bod_disable() \
do { \
  uint8_t tempreg; \
  __asm__ __volatile__("in %[tempreg], %[mcucr]" "\n\t" \
                       "ori %[tempreg], %[bods_bodse]" "\n\t" \
                       "out %[mcucr], %[tempreg]" "\n\t" \
                       "andi %[tempreg], %[not_bodse]" "\n\t" \
                       "out %[mcucr], %[tempreg]" \
                       : [tempreg] "=&d" (tempreg) \
                       : [mcucr] "I" _SFR_IO_ADDR(MCUCR), \
                         [bods_bodse] "i" (_BV(BODS) | _BV(BODSE)), \
                         [not_bodse] "i" (~_BV(BODSE))); \
} while (0)
#endif
