#ifndef __ADC_H
#define __ADC_H	
#include "sys.h"
#include "stm32f10x_adc.h"


void Adc_Init(void);
u16  Get_Adc(u8 ch); 
u16 Get_Adc_Average(u8 ch,u8 times); //多次调用Get_Adc()函数读取ADC值并取平均值；
 
#endif 
