#include "led.h"
#include "delay.h"
#include "sys.h"
#include "lcd.h"
#include "usart.h"
#include "hc05.h"
#include "usart3.h"			 	 
#include "string.h"	   
#include "usmart.h"
#include "adc.h"
#include "measure.h"

//显示ATK-HC05模块的主从状态
void HC05_Role_Show(void)
{
	if(HC05_Get_Role()==1)LCD_ShowString(30,140,200,16,16,"ROLE:Master");	//主机
	else LCD_ShowString(30,140,200,16,16,"ROLE:Slave ");			 		//从机
}
//显示ATK-HC05模块的连接状态
void HC05_Sta_Show(void)
{												 
	if(HC05_LED)LCD_ShowString(120,140,120,16,16,"STA:Connected ");			//连接成功
	else LCD_ShowString(120,140,120,16,16,"STA:Disconnect");	 			//未连接				 
}	

 int main(void)
 {	 
	u8 t=0;
	u8 sendmask=1;
	u16 senddata=0;
	u8 sendbuf[40];
	 float vol=0;	  
	u8 reclen=0;  	
	delay_init();	    	 //延时函数初始化	  
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);	//设置NVIC中断分组2:2位抢占优先级，2位响应优先级
	uart_init(115200);	 	//串口初始化为9600
	LED_Init();				//初始化与LED连接的硬件接口
	LCD_Init();				//初始化LCD
	Adc_Init();				//初始化ADC
	usmart_dev.init(72); 	//初始化USMART		
	 
	POINT_COLOR=RED;
	LCD_ShowString(30,30,200,16,16,"ALIENTEK STM32F1 ^_^");	
	LCD_ShowString(30,50,200,16,16,"HC05 BLUETOOTH COM TEST");	
	LCD_ShowString(30,70,200,16,16,"ATOM@ALIENTEK");
	delay_ms(1000);			//等待蓝牙模块上电稳定
 	while(HC05_Init()) 		//初始化ATK-HC05模块  
	{
		LCD_ShowString(30,90,200,16,16,"ATK-HC05 Error!"); 
		delay_ms(500);
		LCD_ShowString(30,90,200,16,16,"Please Check!!!"); 
		delay_ms(100);
	}
	if(HC05_Get_Role()==1){			//初始化为从模式
		HC05_Set_Cmd("AT+ROLE=0");   
		HC05_Set_Cmd("AT+RESET");
		delay_ms(200);
	}
	LCD_ShowString(30,90,210,16,16,"KEY1:ROLE KEY0:SEND/STOP");  
	LCD_ShowString(30,110,200,16,16,"ATK-HC05 Standby!");  
  	LCD_ShowString(30,160,200,16,16,"Send:");	
	LCD_ShowString(30,180,200,16,16,"Receive:"); 
	POINT_COLOR=BLUE;
	HC05_Role_Show();
	delay_ms(100);
	USART3_RX_STA=0;
 	while(1) 
	{		
		delay_ms(10);
		
		if(t==50)
		{
			if(sendmask)					//定时发送
			{	
				senddata = Get_vol();
				vol = (float)senddata*(3.3/4096);
				printf("the data is: %f V\n",vol);
				sprintf((char*)sendbuf,"ALIENTEK HC05 %f\r\n",vol);
	  			LCD_ShowString(30+40,160,200,16,16,sendbuf);	//显示发送数据	
				u3_printf("%f",vol);		//发送到蓝牙模块
			}
			HC05_Sta_Show();  	  
			t=0;
			LED0=!LED0; 	     
		}	  
		
		if(USART3_RX_STA&0X8000)			//接收到一次数据了
		{
			LCD_Fill(30,200,240,320,WHITE);	//清除显示
 			reclen=USART3_RX_STA&0X7FFF;	//得到数据长度
		  	USART3_RX_BUF[reclen]=0;	 	//加入结束符
			if(reclen==9||reclen==8) 		//控制DS1检测
			{
				if(strcmp((const char*)USART3_RX_BUF,"+LED1 ON")==0)LED1=0;	//打开LED1
				if(strcmp((const char*)USART3_RX_BUF,"+LED1 OFF")==0)LED1=1;//关闭LED1
			}
 			LCD_ShowString(30,200,209,119,16,USART3_RX_BUF);//显示接收到的数据
 			USART3_RX_STA=0;	 
		}	 															     				   
		t++;	
	}
}
