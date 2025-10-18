<template>
    <div class='content'>
		<vue-advanced-chat
			class='chat'
			:singleRoom='true'
			height='100%'
			:current-user-id='currentUserId'
			:rooms='JSON.stringify(rooms)'
			:rooms-loaded='true'
			:messages='JSON.stringify(messages)'
			:messages-loaded='messagesLoaded'
			@send-message='sendMessage($event.detail[0])'
			@fetch-messages='fetchMessages($event.detail[0])'
			:show-files=false
			:show-emojis=false
			message-actions='[]'
			show-reaction-emojis='false'
			:show-new-messages-divider=false
			:load-first-room=false
		/>
	</div>
</template>

<script setup>

import { register } from 'vue-advanced-chat'
import { fetchEventSource } from '@microsoft/fetch-event-source'

register();

const baseApi =  import.meta.env.VITE_APP_BASE_API;

const currentUserId = ref('1234');
const rooms = ref([
  {
    roomId: '1',
    roomName: '智能助手',
    users: [
      { _id: '1234', username: 'John Doe' },
      { _id: '4321', username: 'John Snow' }
    ]
  }
]);

const messages = ref([]);
const messagesLoaded = ref(false);
const conversion_id = ref();

function sendMessage(message, hide_msg) {
  
  if (!hide_msg) {
    messages.value = [
      ...messages.value,
      {
        _id: messages.value.length,
        content: message.content,
        senderId: currentUserId.value,
        timestamp: new Date().toString().substring(16, 21),
        date: new Date().toDateString()
      }
    ]
  }


  const requestData = {
    query: message.content,
    userId: '1475',
    conversionId: conversion_id.value
  }

  const ctrl = new AbortController()
	addNewMessage('思考中...')
  
  let times = 0

  fetchEventSource(baseApi+'/chat', {
    method: 'POST',
    openWhenHidden: true,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive'
    },
    signal: ctrl.signal,
    body: JSON.stringify(requestData),
    onmessage: event => {
      try{

        if (times == 0) {
          messages.value[messages.value.length - 1].content = ''
        }
        times += 1
        const newMessage = JSON.parse(event.data)

        if (newMessage.event == 'message') {

          conversion_id.value = newMessage.conversation_id

          let message = messages.value[messages.value.length - 1]

          if (message.senderId == currentUserId.value) {
            
            addNewMessage(newMessage.answer)

          } else {
            messages.value[messages.value.length - 1].content += newMessage.answer
          }

        }
        else{
          ctrl?.abort()
        }
      }
      catch (e) {
        ctrl?.abort()
      }
    },
    onclose() {
      ctrl?.abort()
    },
    onerror(err) {
      ctrl?.abort()
    }
  });

}

function fetchMessages({ options = {} }) {
  messagesLoaded.value = true;
}

function addNewMessage(message) {
  messages.value = [
    ...messages.value,
    {
      _id: messages.value.length,
      content: message,
      senderId: '4321',
      timestamp: new Date().toString().substring(16, 21),
      date: new Date().toDateString()
    }
  ]
}

</script>

<style lang="scss" scoped>
.content {
	width: 100%;
	height: 99%;
}
</style>