import type { User, Story, Post, Message, Notification, Contact, Comment, FriendUser, FriendRequest } from "@/types";

export const ME: User = {
  name: "Nguyễn Văn An",
  avatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=80&h=80&fit=crop&auto=format",
  cover: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=900&h=300&fit=crop&auto=format",
};

export const STORIES: Story[] = [
  { id: 1, name: "Tin của bạn", avatar: ME.avatar, bg: ME.cover, isOwn: true },
  { id: 2, name: "Minh Anh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=80&h=80&fit=crop&auto=format", bg: "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=120&h=180&fit=crop&auto=format" },
  { id: 3, name: "Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&h=80&fit=crop&auto=format", bg: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=120&h=180&fit=crop&auto=format" },
  { id: 4, name: "Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&h=80&fit=crop&auto=format", bg: "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=120&h=180&fit=crop&auto=format" },
  { id: 5, name: "Văn Đức", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=80&h=80&fit=crop&auto=format", bg: "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=120&h=180&fit=crop&auto=format" },
];

export const POSTS: Post[] = [
  { id: 1, user: "Nguyễn Minh Anh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=80&h=80&fit=crop&auto=format", time: "2 giờ trước", content: "Hôm nay trời Hà Nội đẹp quá! ☀️ Đi dạo quanh Hồ Tây thật tuyệt vời. Mọi người có ai rảnh không cùng đi cà phê không?", image: "https://images.unsplash.com/photo-1555252333-9f8e92e65df9?w=600&h=400&fit=crop&auto=format", likes: 124, comments: 38, shares: 12, liked: false },
  { id: 2, user: "Trần Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&h=80&fit=crop&auto=format", time: "4 giờ trước", content: "Vừa xem xong bộ phim mới, hay tuyệt vời! Ai chưa xem thì nên xem ngay nhé. Không spoil nhưng kết thúc rất bất ngờ 😱", image: null, likes: 87, comments: 22, shares: 5, liked: true },
  { id: 3, user: "Lê Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&h=80&fit=crop&auto=format", time: "6 giờ trước", content: "Cuối tuần này mọi người có kế hoạch gì chưa? Mình đang tính đi Đà Lạt nhưng chưa có bạn đồng hành 🌸", image: "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=600&h=400&fit=crop&auto=format", likes: 215, comments: 67, shares: 18, liked: false },
];

export const MESSAGES: Message[] = [
  { id: 1, name: "Minh Anh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=60&h=60&fit=crop&auto=format", text: "Oke bạn nhé, hẹn gặp sau!", time: "2 ph", online: true, unread: 2 },
  { id: 2, name: "Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&h=60&fit=crop&auto=format", text: "Bạn đã xem phim chưa?", time: "15 ph", online: true, unread: 0 },
  { id: 3, name: "Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=60&h=60&fit=crop&auto=format", text: "Cùng đi Đà Lạt nha! 🌸", time: "1 giờ", online: false, unread: 1 },
  { id: 4, name: "Văn Đức", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=60&h=60&fit=crop&auto=format", text: "OK anh, em sẽ gửi file sau", time: "3 giờ", online: true, unread: 0 },
];

export const NOTIFICATIONS: Notification[] = [
  { id: 1, avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=60&h=60&fit=crop&auto=format", text: "Minh Anh đã thích ảnh của bạn.", time: "5 phút trước", read: false },
  { id: 2, avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&h=60&fit=crop&auto=format", text: "Thanh Hùng đã bình luận bài viết của bạn: \"Hay quá bạn ơi!\"", time: "20 phút trước", read: false },
  { id: 3, avatar: "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=60&h=60&fit=crop&auto=format", text: "Phạm Thị Bích đã gửi lời mời kết bạn cho bạn.", time: "1 giờ trước", read: false },
  { id: 4, avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=60&h=60&fit=crop&auto=format", text: "Lan Phương đã chia sẻ bài viết của bạn.", time: "3 giờ trước", read: true },
  { id: 5, avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=60&h=60&fit=crop&auto=format", text: "Văn Đức đã tag bạn trong một bài viết.", time: "hôm qua", read: true },
];

export const COMMENTS: Comment[] = [
  // Post 1 comments
  { id: 1, postId: 1, parentId: null, user: "Trần Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&h=60&fit=crop&auto=format", content: "Uh, Hồ Tây đẹp lắm! Tuần sau mình cũng định ra đó đây 😄", time: "1 giờ trước", likes: 12, liked: false },
  { id: 2, postId: 1, parentId: null, user: "Lê Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=60&h=60&fit=crop&auto=format", content: "Trời hôm nay đẹp thật, mình cũng đang ngồi cà phê đây 🌤️", time: "45 phút trước", likes: 5, liked: false },
  { id: 3, postId: 1, parentId: 1, user: "Nguyễn Minh Anh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=60&h=60&fit=crop&auto=format", content: "Vậy tuần sau mình cùng đi nhé Hùng! 😊", time: "30 phút trước", likes: 3, liked: false },
  { id: 4, postId: 1, parentId: 1, user: "Văn Đức", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=60&h=60&fit=crop&auto=format", content: "Cho mình đi với! Lâu rồi chưa ra Hồ Tây", time: "20 phút trước", likes: 2, liked: false },
  // Post 2 comments
  { id: 5, postId: 2, parentId: null, user: "Lê Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=60&h=60&fit=crop&auto=format", content: "Phim gì vậy bạn? Spoil nhẹ cho mình với 😂", time: "3 giờ trước", likes: 8, liked: false },
  { id: 6, postId: 2, parentId: null, user: "Nguyễn Văn An", avatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=60&h=60&fit=crop&auto=format", content: "Hay không? Mình đang phân vân xem không 🤔", time: "2 giờ trước", likes: 4, liked: true },
  { id: 7, postId: 2, parentId: 5, user: "Trần Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&h=60&fit=crop&auto=format", content: "Không spoil được, xem đi sẽ biết ngay thôi haha 😅", time: "2 giờ 30 phút trước", likes: 6, liked: false },
  // Post 3 comments
  { id: 8, postId: 3, parentId: null, user: "Văn Đức", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=60&h=60&fit=crop&auto=format", content: "Mình đi được nè! Đà Lạt tháng này đẹp lắm 🌸", time: "5 giờ trước", likes: 15, liked: false },
  { id: 9, postId: 3, parentId: null, user: "Trần Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&h=60&fit=crop&auto=format", content: "Cuối tuần mình cũng rảnh, count me in! 🙋", time: "4 giờ trước", likes: 9, liked: false },
  { id: 10, postId: 3, parentId: 8, user: "Lê Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=60&h=60&fit=crop&auto=format", content: "Oke bạn! Mình sẽ book khách sạn nhé 😍", time: "3 giờ trước", likes: 7, liked: false },
  { id: 11, postId: 3, parentId: 8, user: "Nguyễn Minh Anh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=60&h=60&fit=crop&auto=format", content: "Mình cũng muốn đi! Còn chỗ không Phương? 🥺", time: "2 giờ trước", likes: 4, liked: false },
];

export const FRIEND_USERS: FriendUser[] = [
  { id: 1, name: "Nguyễn Minh Anh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=900&h=300&fit=crop&auto=format", mutualFriends: 14, location: "Hà Nội", work: "Viettel", education: "Đại học Bách Khoa HN", bio: "Yêu thiên nhiên và cà phê sáng ☕" },
  { id: 2, name: "Trần Thanh Hùng", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=900&h=300&fit=crop&auto=format", mutualFriends: 8, location: "TP. Hồ Chí Minh", work: "VNG Corporation", education: "Đại học KHTN", bio: "Đam mê phim ảnh và âm nhạc 🎬🎵" },
  { id: 3, name: "Lê Lan Phương", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=900&h=300&fit=crop&auto=format", mutualFriends: 22, location: "Đà Nẵng", work: "Freelancer", education: "Đại học Kinh tế Đà Nẵng", bio: "Thích du lịch và khám phá ẩm thực 🌍" },
  { id: 4, name: "Văn Đức", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=900&h=300&fit=crop&auto=format", mutualFriends: 5, location: "Hà Nội", work: "FPT Software", education: "Học viện Công nghệ Bưu chính", bio: "Lập trình viên, gamer 🎮" },
  { id: 5, name: "Thu Hà", avatar: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=900&h=300&fit=crop&auto=format", mutualFriends: 11, location: "Hải Phòng", work: "Giáo viên", education: "Đại học Sư phạm HN", bio: "Yêu trẻ em và nghề giáo 📚" },
  { id: 6, name: "Quang Khải", avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=900&h=300&fit=crop&auto=format", mutualFriends: 3, location: "Huế", work: "Kiến trúc sư", education: "Đại học Kiến trúc HN", bio: "" },
  { id: 7, name: "Bảo Ngọc", avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1555252333-9f8e92e65df9?w=900&h=300&fit=crop&auto=format", mutualFriends: 17, location: "Hà Nội", work: "Marketing Manager", education: "Đại học Ngoại thương", bio: "Creative & passionate 🌸" },
  { id: 8, name: "Phạm Thị Bích", avatar: "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=900&h=300&fit=crop&auto=format", mutualFriends: 12, location: "Hà Nội", work: "Bác sĩ", education: "Đại học Y Hà Nội", bio: "Sức khỏe là vốn quý 🏥" },
  { id: 9, name: "Nguyễn Hữu Tài", avatar: "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=900&h=300&fit=crop&auto=format", mutualFriends: 7, location: "TP. Hồ Chí Minh", work: "Kỹ sư phần mềm", education: "Đại học CNTT TP.HCM", bio: "" },
  { id: 10, name: "Minh Khoa", avatar: "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=160&h=160&fit=crop&auto=format", cover: "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=900&h=300&fit=crop&auto=format", mutualFriends: 2, location: "Cần Thơ", work: "Doanh nhân", education: "Đại học Cần Thơ", bio: "Entrepreneur & coffee lover ☕" },
];

// ids 1–5 are friends; 8 & 9 are pending incoming requests
export const INITIAL_FRIENDS: number[] = [1, 2, 3, 4, 5];
export const INITIAL_FOLLOWING: number[] = [1, 2, 3, 4, 5, 7];
export const INITIAL_PENDING_RECEIVED: number[] = [8, 9];
export const INITIAL_PENDING_SENT: number[] = [];

export const FRIEND_REQUESTS: FriendRequest[] = [
  { id: 1, from: FRIEND_USERS[7], time: "1 giờ trước" },
  { id: 2, from: FRIEND_USERS[8], time: "3 giờ trước" },
];

export const PEOPLE_YOU_MAY_KNOW: FriendUser[] = [FRIEND_USERS[5], FRIEND_USERS[6], FRIEND_USERS[9]];

export const CONTACTS: Contact[] = [
  { id: 1, name: "Văn Đức", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=60&h=60&fit=crop&auto=format", online: true },
  { id: 2, name: "Thu Hà", avatar: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=60&h=60&fit=crop&auto=format", online: true },
  { id: 3, name: "Quang Khải", avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=60&h=60&fit=crop&auto=format", online: false },
  { id: 4, name: "Bảo Ngọc", avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=60&h=60&fit=crop&auto=format", online: true },
  { id: 5, name: "Minh Khoa", avatar: "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=60&h=60&fit=crop&auto=format", online: false },
  { id: 6, name: "Hồng Nhung", avatar: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=60&h=60&fit=crop&auto=format", online: true },
];
