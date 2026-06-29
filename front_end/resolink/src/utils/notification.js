// 通用桌面通知工具

// 检查浏览器是否支持桌面通知
const isNotificationSupported = () => {
  return 'Notification' in window;
};

// 请求通知权限
const requestNotificationPermission = async () => {
  if (!isNotificationSupported()) {
    console.log('浏览器不支持桌面通知');
    return false;
  }

  if (Notification.permission === 'granted') {
    return true;
  }

  if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission();
    return permission === 'granted';
  }

  return false;
};

// 发送桌面通知
const sendNotification = (title, options = {}) => {
  
  if (!isNotificationSupported()) {
    console.log('浏览器不支持桌面通知');
    return false;
  }

  if (Notification.permission === 'granted') {
    try {
      // 添加默认图标和振动效果
      const defaultOptions = {
        icon: '/favicon.ico', // 可以替换为实际的图标路径
        vibrate: [200, 100, 200], // 可选的振动模式
        requireInteraction: true // 手动关闭通知
      };
      
      const notification = new Notification(title, { ...defaultOptions, ...options });

      // 自动关闭通知
      // if (!options.requireInteraction) {
      //   setTimeout(() => notification.close(), 5000);
      // }
      
      return true;
    } catch (error) {
      console.error('发送通知失败:', error);
      return false;
    }
  } else {
    console.log('未获得通知权限，当前权限:', Notification.permission);
    return false;
  }
};

// 获取通知权限状态
const getNotificationPermission = () => {
  if (!isNotificationSupported()) {
    return 'unsupported';
  }
  return Notification.permission;
};

export {
  isNotificationSupported,
  requestNotificationPermission,
  sendNotification,
  getNotificationPermission
};