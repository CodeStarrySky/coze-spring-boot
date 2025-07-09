import request from "@/base/http/request";

export const chat = (
  chatId: string,
  userMessage: string,
  handle: Function,
) => {};
export const cancel = (chatId: string) => {
    return request({
        url: "/assistant/cancel",
        method: "post",
        params: {chatId: chatId},
    });
};