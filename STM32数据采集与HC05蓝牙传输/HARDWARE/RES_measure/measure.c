#include "measure.h"

u16 Get_vol(void){
	
	u16 vol;
	vol = Get_Adc(ADC_Channel_1);
	return vol;
}

