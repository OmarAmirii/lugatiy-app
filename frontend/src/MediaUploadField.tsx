import { useRef, useState } from 'react';
import { CheckCircle2, CloudUpload, FileVideo, FileText, Link2, X } from 'lucide-react';

export type MediaSelection = { mode:'file'; file:File } | { mode:'url'; url:string } | null;
type Props = { kind:'pdf'|'mp4'; value:MediaSelection; onChange:(value:MediaSelection)=>void; disabled?:boolean };

export function MediaUploadField({kind,value,onChange,disabled=false}:Props){
 const [mode,setMode]=useState<'file'|'url'>('file');const [dragging,setDragging]=useState(false);const [error,setError]=useState('');const input=useRef<HTMLInputElement>(null);
 const label=kind==='pdf'?'PDF':'MP4';const max=kind==='pdf'?25:500;const Icon=kind==='pdf'?FileText:FileVideo;
 function choose(file:File|undefined){setError('');if(!file)return;if(!file.name.toLowerCase().endsWith(`.${kind}`)){onChange(null);setError(`Kies een ${label}-bestand.`);return;}if(file.size>max*1024*1024){onChange(null);setError(`Het bestand mag maximaal ${max} MB zijn.`);return;}onChange({mode:'file',file});}
 function switchMode(next:'file'|'url'){setMode(next);onChange(null);setError('');if(input.current)input.current.value='';}
 return <fieldset className="media-upload" disabled={disabled}><legend>{label}-bestand</legend><div className="media-source-tabs" role="group" aria-label={`Bron ${label}-bestand`}><button type="button" className={mode==='file'?'active':''} aria-pressed={mode==='file'} onClick={()=>switchMode('file')}><CloudUpload size={16}/> Bestand uploaden</button><button type="button" className={mode==='url'?'active':''} aria-pressed={mode==='url'} onClick={()=>switchMode('url')}><Link2 size={16}/> Via URL</button></div>{mode==='file'?<div className={`media-dropzone${dragging?' dragging':''}${value?.mode==='file'?' has-file':''}`} onDragEnter={e=>{e.preventDefault();setDragging(true);}} onDragOver={e=>{e.preventDefault();e.dataTransfer.dropEffect='copy';}} onDragLeave={e=>{if(!e.currentTarget.contains(e.relatedTarget as Node))setDragging(false);}} onDrop={e=>{e.preventDefault();setDragging(false);choose(e.dataTransfer.files[0]);}}>{value?.mode==='file'?<><span className="media-upload-icon"><Icon size={24}/></span><strong>{value.file.name}</strong><small>{(value.file.size/1024/1024).toFixed(1)} MB · klaar om te uploaden</small><button className="media-clear" type="button" onClick={()=>{onChange(null);if(input.current)input.current.value='';}}><X size={14}/> Bestand verwijderen</button></>:<><span className="media-upload-icon"><CloudUpload size={24}/></span><strong>Sleep je {label}-bestand hierheen</strong><small>of kies een bestand van je apparaat · maximaal {max} MB</small><button className="media-browse" type="button" onClick={()=>input.current?.click()}>Bestand kiezen</button></>}<input ref={input} className="sr-only" type="file" aria-label={`${label}-bestand kiezen`} accept={kind==='pdf'?'application/pdf,.pdf':'video/mp4,.mp4'} onChange={e=>choose(e.target.files?.[0])}/></div>:<div className="media-url"><label htmlFor={`media-url-${kind}`}>Link naar {label}-bestand</label><div className="media-url-input"><Link2 size={17}/><input id={`media-url-${kind}`} type="url" placeholder={`https://voorbeeld.nl/bestand.${kind}`} value={value?.mode==='url'?value.url:''} onChange={e=>onChange({mode:'url',url:e.target.value})}/></div><small>Gebruik een directe HTTPS-link. De website moet toestaan dat je browser het bestand ophaalt.</small></div>}{error&&<p className="media-error" role="alert">{error}</p>}{value?.mode==='file'&&<span className="sr-only" role="status"><CheckCircle2/> Bestand geselecteerd</span>}</fieldset>;
}

export async function resolveMedia(selection:MediaSelection,kind:'pdf'|'mp4'):Promise<File>{
 const max=(kind==='pdf'?25:500)*1024*1024;const name=kind==='pdf'?'document.pdf':'video.mp4';
 if(!selection)throw new Error('Kies een bestand of vul een directe bestandslink in.');
 if(selection.mode==='file')return selection.file;
 let url:URL;try{url=new URL(selection.url.trim());}catch{throw new Error('Vul een geldige HTTPS-link naar het bestand in.');}
 if(url.protocol!=='https:'||url.username||url.password)throw new Error('Gebruik een HTTPS-link zonder gebruikersnaam of wachtwoord.');
 let response:Response;try{response=await fetch(url.toString(),{credentials:'omit',redirect:'error'});}catch{throw new Error('De link kon niet worden opgehaald. Controleer of deze direct naar het bestand verwijst en of de website toegang vanuit deze browser toestaat.');}
 if(!response.ok)throw new Error(`De bestandslink gaf een fout terug (${response.status}).`);
 const length=Number(response.headers.get('content-length'));if(length>max){await response.body?.cancel();throw new Error(`Het bestand is groter dan ${max/1024/1024} MB.`);}
 if(!response.body)throw new Error('Het bestand kon niet worden gelezen.');
 const reader=response.body.getReader();const parts:Uint8Array<ArrayBuffer>[]=[];let size=0;
 try{while(true){const {done,value}=await reader.read();if(done)break;if(value){size+=value.byteLength;if(size>max){await reader.cancel();throw new Error(`Het bestand is groter dan ${max/1024/1024} MB.`);}parts.push(value as Uint8Array<ArrayBuffer>);}}}finally{reader.releaseLock();}
 if(!size)throw new Error('Het opgehaalde bestand is leeg.');
 return new File(parts,name,{type:kind==='pdf'?'application/pdf':'video/mp4'});
}
