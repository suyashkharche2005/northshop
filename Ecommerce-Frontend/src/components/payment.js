import API from '../axios';
const loadCheckout=()=>new Promise((resolve,reject)=>{
 if(window.Razorpay){resolve();return;}
 const script=document.createElement('script');script.src='https://checkout.razorpay.com/v1/checkout.js';
 script.onload=resolve;script.onerror=()=>reject(new Error('Could not load test payment checkout'));
 document.body.appendChild(script);
});
export async function openTestCheckout(details,onSuccess){
 await loadCheckout();
 return new Promise((resolve,reject)=>{
  let handled=false;
  const checkout=new window.Razorpay({
   key:details.keyId,amount:details.amount,currency:details.currency,order_id:details.gatewayOrderId,
   name:'Northshop — test payment',description:'Test mode only. No money is charged.',
   handler:async result=>{
    handled=true;
    try{await API.post(`/payments/test/${details.orderId}/confirm`,{
     gatewayOrderId:result.razorpay_order_id,paymentId:result.razorpay_payment_id,signature:result.razorpay_signature
    });await onSuccess();resolve(true);}catch(e){reject(e);}
   },modal:{ondismiss:()=>{if(!handled)resolve(false);}}
  });
  checkout.on('payment.failed',()=>resolve(false));checkout.open();
 });
}
