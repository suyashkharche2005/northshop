import { createContext, useEffect, useState } from 'react';
const AppContext = createContext(null);
const read = (key, fallback) => { try { return JSON.parse(localStorage.getItem(key)) ?? fallback; } catch { return fallback; } };
export function AppProvider({ children }) {
 const [user,setUser] = useState(() => {try{return JSON.parse(sessionStorage.getItem('user'));}catch{return null;}});
 const [cart,setCart] = useState(() => read('cart', []));
 useEffect(() => {localStorage.setItem('cart',JSON.stringify(cart));},[cart]);
 useEffect(() => {const expire=()=>setUser(null);window.addEventListener('auth-expired',expire);return()=>window.removeEventListener('auth-expired',expire);},[]);
 const login = data => {sessionStorage.setItem('token',data.token);const info={username:data.username,role:data.role};sessionStorage.setItem('user',JSON.stringify(info));setUser(info);};
 const logout = () => {sessionStorage.removeItem('token');sessionStorage.removeItem('user');setUser(null);};
 const addToCart = product => {if(!product.productAvailable||product.stockQuantity<1)return;setCart(items=>{const old=items.find(i=>i.id===product.id);return old?items.map(i=>i.id===product.id?{...i,quantity:Math.min(i.quantity+1,product.stockQuantity)}:i):[...items,{id:product.id,quantity:1}];});};
 const setQuantity=(id,quantity)=>setCart(items=>items.map(i=>i.id===id?{...i,quantity:Math.max(1,quantity)}:i));
 const removeFromCart=id=>setCart(items=>items.filter(i=>i.id!==id));
 return <AppContext.Provider value={{user,login,logout,isAdmin:user?.role==='ADMIN',cart,addToCart,setQuantity,removeFromCart,clearCart:()=>setCart([])}}>{children}</AppContext.Provider>;
}
export default AppContext;
