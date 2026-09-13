import { useState, useEffect, useRef } from "react";
import { usePostStore } from "@/stores/postStore";
import { useUserStore } from "@/stores/userStore";
import type { Post } from "@/types";
import { getMyFriends, type FriendshipItem } from "@/services/friendsService";
import { useAuth } from "@/providers/AuthProvider";
import EmojiPicker, { EmojiStyle, Theme } from "emoji-picker-react";

interface Props {
  onClose: () => void;
  editPost?: Post;
}

const EMOJI_QUICK = ["😊", "❤️", "😂", "🔥", "👍", "🎉", "😍", "🙏"];

const BG_COLORS = [
  null,
  "linear-gradient(135deg,#1877F2,#42B72A)",
  "linear-gradient(135deg,#F5533D,#FF9A00)",
  "linear-gradient(135deg,#833AB4,#FD1D1D,#FCB045)",
  "linear-gradient(135deg,#0F2027,#203A43,#2C5364)",
  "linear-gradient(135deg,#11998e,#38ef7d)",
];

export default function CreatePostModal({ onClose, editPost }: Props) {
  const { addPost, updatePost } = usePostStore();
  const { profile } = useUserStore();
  const { user } = useAuth();

  let initContent = editPost?.content ?? "";
  let initBg = null;
  let initLoc = null;

  const bgMatch = initContent.match(/^\[BG:(.+?)\]\n/);
  if (bgMatch) {
    initBg = bgMatch[1];
    initContent = initContent.replace(bgMatch[0], "");
  }

  const locMatch = initContent.match(/^\[LOC:(.+?)\]\n/);
  if (locMatch) {
    initLoc = locMatch[1];
    initContent = initContent.replace(locMatch[0], "");
  }
  if (initContent.startsWith("[BG:")) {
    const match = initContent.match(/^\[BG:(.+?)\]\n(.*)/s);
    if (match) {
      initBg = match[1];
      initContent = match[2];
    }
  }

  const isEdit = !!editPost;

  const [content, setContent] = useState(initContent);
  const [imageUrl, setImageUrl] = useState(editPost?.image ?? "");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [showImageInput, setShowImageInput] = useState(!!editPost?.image);
  const [bgGradient, setBgGradient] = useState<string | null>(initBg);

  const canSubmit = content.trim().length > 0 || imageUrl.length > 0 || imageFile !== null;

  const [visibility, setVisibility] = useState(editPost?.visibility ?? "PUBLIC");
  const [showPrivacy, setShowPrivacy] = useState(false);
  const PRIVACY_OPTIONS = [
    { 
      value: "PUBLIC", label: "Công khai", 
      icon: <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24">
              <path d={
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93" +
                "c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm" +
                "6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45" +
                " 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"
              }/>
            </svg> 
    },
    { 
      value: "FRIENDS", label: "Bạn bè", 
      icon: <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24">
              <path d={
                "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 " +
                "0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 " +
                "0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 " +
                "1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"
              }/>
            </svg> 
    },
    { 
      value: "PRIVATE", label: "Chỉ mình tôi", 
      icon: <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24">
              <path d={
                "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 " +
                "2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 " +
                "2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
              }/>
            </svg> 
    }
  ];
  const currentPrivacy = PRIVACY_OPTIONS.find(p => p.value === visibility);
  const [showEmoji, setShowEmoji] = useState(false);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [location, setLocation] = useState<string | null>(initLoc);
  const [showLocationInput, setShowLocationInput] = useState(!!initLoc);

  // States for Location Search Feature
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isSearchingLoc, setIsSearchingLoc] = useState(false);
  const searchTimeout = useRef<NodeJS.Timeout | null>(null);

  // States for Tagging Friends
  const [showTagFriends, setShowTagFriends] = useState(false);
  const [friendsList, setFriendsList] = useState<FriendshipItem[]>([]);
  const [taggedUserIds, setTaggedUserIds] = useState<string[]>(editPost?.taggedUserIds || []);

  useEffect(() => {
    if (showTagFriends && friendsList.length === 0) {
      getMyFriends().then(res => setFriendsList(res)).catch(err => console.error(err));
    }
  }, [showTagFriends]);

  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  function handleSubmit() {
    if (!canSubmit) return;

    // Nối lên mã màu nền vào đầu văn bản trước khi gửi xuống Server
    let finalContent = content.trim();

    // Ghép Background vào trước
    if (bgGradient) {
      finalContent = `[BG:` + bgGradient + `]\n` + finalContent;
    }

    // Ghép Vị trí vào đầu chuỗi
    if (location && location.trim().length > 0) {
      finalContent = `[LOC:` + location.trim() + `]\n` + finalContent;
    }

    if (isEdit) {
      updatePost(editPost!.id, finalContent, imageFile || imageUrl, visibility, taggedUserIds);
    } else {
      addPost(finalContent, imageFile || imageUrl, visibility, taggedUserIds);
    }
    onClose();
  }

    function handleCurrentLocation() {
      if (!navigator.geolocation) {
        alert("Your browser does not support Geolocation!");
        return;
      }
      setIsSearchingLoc(true);
      navigator.geolocation.getCurrentPosition(async (pos) => {
        try {
          // Use OpenStreetMap API to reverse-geocode coordinates into address
          const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=` + pos.coords.latitude + `&lon=` + pos.coords.longitude);
          const data = await res.json();
          const placeName = data.display_name.split(",")[0]; // Get the most specific area name
          setLocation(placeName);
          setSearchResults([]);
        } catch (err) {
          alert("Error fetching location!");
        } finally {
          setIsSearchingLoc(false);
        }
      }, () => {
        alert("Permission to access location was denied!");
        setIsSearchingLoc(false);
      });
    }
  

    // Handle typing to search location (with 500ms debounce)
    function handleSearchLocation(query: string) {
      setLocation(query);
      if (query.trim().length < 3) {
        setSearchResults([]);
        return;
      }

      if (searchTimeout.current) clearTimeout(searchTimeout.current);

      searchTimeout.current = setTimeout(async () => {
        setIsSearchingLoc(true);
        try {
          // Use OpenStreetMap API to search places by name
          const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=` + query + `&limit=5`);
          const data = await res.json();
          setSearchResults(data);
        } catch (err) {
          console.error("Error finding location:", err);
        } finally {
          setIsSearchingLoc(false);
        }
      }, 500);
    }

    function insertEmoji(emoji: string) {
      const el = textareaRef.current;
      if (!el) return;
      const start = el.selectionStart;
      const end = el.selectionEnd;
      const newVal = content.slice(0, start) + emoji + content.slice(end);
      setContent(newVal);
      setTimeout(() => {
        el.selectionStart = el.selectionEnd = start + emoji.length;
        el.focus();
      }, 0);
    }

    // Function to get tagged friends names
    const getTaggedNames = () => {
      if (taggedUserIds.length === 0) return "";
      const taggedFriends = taggedUserIds.map(id => {
        const friendItem = friendsList.find(f => f.user.id === id || f.friend.id === id);
        if (friendItem) {
          return friendItem.user.id === user?.id ? friendItem.friend.displayName : friendItem.user.displayName;
        }
        return "Người dùng";
      });
      
      if (taggedFriends.length === 1) return ` cùng với ${taggedFriends[0]}`;
      if (taggedFriends.length === 2) return ` cùng với ${taggedFriends[0]} và ${taggedFriends[1]}`;
      if (taggedFriends.length === 3) return ` cùng với ${taggedFriends[0]}, ${taggedFriends[1]} và ${taggedFriends[2]}`;
      return ` cùng với ${taggedFriends[0]}, ${taggedFriends[1]}, ${taggedFriends[2]} và ${taggedFriends.length - 3} người khác`;
    };

    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center">
        {/* Backdrop */}
        <div className="absolute inset-0 bg-black/50" onClick={onClose} />

        {/* Modal */}
        <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-[520px] mx-4">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-[#E4E6EB]">
            <div className="w-9" />
            <h2 className="font-bold text-[#1C1E21] text-lg">
              {isEdit ? "Chỉnh sửa bài viết" : "Tạo bài viết"}
            </h2>
            <button
              onClick={onClose}
              className="w-9 h-9 rounded-full bg-[#E4E6EB] hover:bg-[#D8DADF] flex items-center justify-center transition-colors text-[#1C1E21]"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>
          </div>

          {/* Author */}
          <div className="flex items-center gap-3 px-4 pt-3 pb-1">
            <img src={profile.avatar} alt="me" className="w-10 h-10 rounded-full object-cover" />
            <div>
              <p className="font-semibold text-[#1C1E21] text-sm">
                <span className="font-bold">{profile.name}</span>
                <span className="font-normal text-[#65676B]">
                  {getTaggedNames()}
                </span>
                {location && (
                  <span className="text-[#65676B] font-normal">
                    {" "}đang ở <span className="font-semibold text-[#1C1E21]">{location}</span>
                  </span>
                )}
              </p>
              <div className="relative mt-1">
                <button
                  onClick={() => setShowPrivacy(!showPrivacy)}
                  className="flex items-center gap-1 bg-[#E4E6EB] hover:bg-[#D8DADF] text-[#1C1E21] text-xs font-semibold px-2 py-0.5 rounded-md transition-colors"
                >
                  <span>{currentPrivacy?.icon}</span>
                  {currentPrivacy?.label}
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {showPrivacy && (
                  <div className="absolute top-full left-0 mt-1 w-36 bg-white border border-[#E4E6EB] shadow-xl rounded-lg py-1 z-50">
                    {PRIVACY_OPTIONS.map(opt => (
                      <button
                        key={opt.value}
                        onClick={() => { setVisibility(opt.value); setShowPrivacy(false); }}
                        className="flex items-center gap-2 w-full px-3 py-2 text-sm hover:bg-[#F0F2F5] text-left"
                      >
                        <span>{opt.icon}</span> {opt.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div
            className="mx-4 rounded-xl overflow-hidden mb-2 transition-all"
            style={bgGradient ? { background: bgGradient } : {}}
          >
            <textarea
              ref={textareaRef}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder={`${profile.name ? profile.name.trim().split(" ").pop() : ""} ơi, bạn đang nghĩ gì thế?`}
              rows={bgGradient ? 4 : 3}
              className={`w-full resize-none outline-none text-[#1C1E21] placeholder-[#65676B] leading-relaxed transition-all ${bgGradient
                ? "bg-transparent text-white placeholder-white/70 text-3xl font-bold text-center py-20 px-6" : "bg-transparent text-base p-2"
                }`}
            />
          </div>

          {/* Background color picker */}
          {!showImageInput && (
            <div className="px-4 mb-2 flex items-center gap-2">
              {BG_COLORS.map((bg, i) => (
                  <button
                    key={i}
                    onClick={() => setBgGradient(bg === bgGradient ? null : bg)}
                    className={
                      `w-8 h-8 rounded-md transition-all hover:scale-105 ` +
                      (bgGradient === bg 
                        ? "ring-2 ring-offset-2 ring-[#1877F2] scale-105" 
                        : "border border-gray-200 shadow-sm")
                    }
                    style={{ background: bg ?? "#E4E6EB" }}
                    title={bg ? "Nền màu" : "Không nền"}
                  >
                    {!bg && <span className="text-xs text-[#65676B] flex items-center justify-center h-full font-bold">Aa</span>}
                  </button>
              ))}
            </div>
          )}

          {/* Image URL input */}
          {showImageInput && (
            <div className="mx-4 mb-3 border border-[#CED0D4] rounded-lg p-2 relative">
              {/* Nút đóng vùng chọn ảnh */}
              <button
                onClick={() => { setShowImageInput(false); setImageUrl(""); setImageFile(null); setActiveTab(null); }}
                className={
                  "absolute top-4 right-4 z-10 w-7 h-7 bg-white border " +
                  "border-[#CED0D4] rounded-full flex items-center justify-center " +
                  "hover:bg-[#E4E6EB] transition-colors"
                }
                title="Đóng"
              >
                <svg className="w-4 h-4 text-[#65676B]" fill="currentColor" viewBox="0 0 24 24">
                  <path d={
                    "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 " +
                    "6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
                  } />
                </svg>
              </button>

              {!imageUrl ? (
                <label
                  className={
                    "bg-[#F7F8FA] hover:bg-[#F0F2F5] transition-colors rounded-lg " +
                    "p-6 flex flex-col items-center justify-center min-h-[200px] " +
                    "cursor-pointer border border-transparent hover:border-[#CED0D4]"
                  }
                >
                  <div className="flex items-center gap-3 mb-3">
                    <div
                      className={
                        "w-12 h-12 bg-[#E4E6EB] rounded-full flex items-center " +
                        "justify-center shadow-sm"
                      }
                    >
                      <svg className="w-7 h-7 text-[#1C1E21]" fill="currentColor" viewBox="0 0 24 24">
                        <path d={
                          "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 " +
                          "0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
                        } />
                      </svg>
                    </div>

                    <div
                      className={
                        "w-12 h-12 bg-[#E4E6EB] rounded-full flex items-center " +
                        "justify-center shadow-sm"
                      }
                    >
                      <svg className="w-7 h-7 text-[#1C1E21]" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
                      </svg>
                    </div>
                  </div>
                  <span className="font-semibold text-[#1C1E21] text-[15px]">
                    Thêm ảnh/video
                  </span>
                  <span className="text-[13px] text-[#65676B]">
                    hoặc kéo thả vào đây
                  </span>
                  {/* Trình tải file ẩn */}
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        setImageFile(file);
                        const reader = new FileReader();
                        reader.onload = (ev) => {
                          setImageUrl(ev.target?.result as string);
                        };
                        reader.readAsDataURL(file);
                      }
                    }}
                  />
                </label>
              ) : (
                <div className="relative rounded-lg overflow-hidden bg-[#F0F2F5] flex justify-center">
                  <img
                    src={imageUrl}
                    alt="preview"
                    className="w-full max-h-[300px] object-contain"
                  />
                </div>
              )}
            </div>
          )}

          {/* Tag Friends UI */}
          {showTagFriends && (
            <div className="mx-4 mb-3 p-3 border border-[#CED0D4] rounded-lg max-h-60 overflow-y-auto">
              <h3 className="font-semibold text-sm mb-2">Gắn thẻ người khác</h3>
              {friendsList.length === 0 ? (
                <p className="text-sm text-gray-500">Đang tải bạn bè...</p>
              ) : (
                <div className="flex flex-col gap-2">
                  {friendsList.map(f => {
                    const friendId = f.user.id === user?.id ? f.friend.id : f.user.id;
                    const friendName = f.user.id === user?.id ? f.friend.displayName : f.user.displayName;
                    const friendAvatar = f.user.id === user?.id ? f.friend.avatarUrl : f.user.avatarUrl;
                    const isTagged = taggedUserIds.includes(friendId);
                    return (
                      <div key={f.id} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded-lg cursor-pointer" onClick={() => {
                        if (isTagged) {
                          setTaggedUserIds(prev => prev.filter(id => id !== friendId));
                        } else {
                          setTaggedUserIds(prev => [...prev, friendId]);
                        }
                      }}>
                        <div className="flex items-center gap-2">
                          <img src={friendAvatar || "https://ui-avatars.com/api/?name=" + friendName} className="w-8 h-8 rounded-full" />
                          <span className="text-sm font-medium">{friendName}</span>
                        </div>
                        <input type="checkbox" checked={isTagged} readOnly className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* Location input with Search & GPS */}
          {showLocationInput && (
            <div className="mx-4 mb-3 relative">
              <div
                className={
                  "flex items-center gap-2 bg-[#F0F2F5] rounded-full " +
                  "px-4 py-2 border border-transparent " +
                  "focus-within:border-[#CED0D4] transition-colors"
                }
              >
                <svg
                  className="w-5 h-5 text-[#65676B] flex-shrink-0"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d={
                    "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 " +
                    "13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 " +
                    "4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 " +
                    "14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
                  } />
                </svg>

                <input
                  type="text"
                  placeholder="Tìm kiếm vị trí..."
                  value={location ?? ""}
                  onChange={(e) => handleSearchLocation(e.target.value)}
                  className={
                    "w-full bg-transparent outline-none text-[15px] " +
                    "text-[#1C1E21] placeholder-[#65676B]"
                  }
                />

                {/* GPS Button */}
                <button
                  onClick={handleCurrentLocation}
                  title="Sử dụng vị trí hiện tại"
                  className={
                    "flex-shrink-0 p-1.5 hover:bg-[#E4E6EB] rounded-full " +
                    "text-[#65676B] transition-colors"
                  }
                >
                  {isSearchingLoc ? (
                    <span
                      className={
                        "w-5 h-5 block border-2 border-red-500 " +
                        "border-t-transparent rounded-full animate-spin"
                      }
                    ></span>
                  ) : (
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                      <path d={
                        "M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4" +
                        "-1.79-4-4-4zm8.94 3c-.46-4.17-3.77-7.48-7.94-7.94" +
                        "V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06" +
                        "c.46 4.17 3.77 7.48 7.94 7.94V23h2v-2.06c4.17-.46 " +
                        "7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7" +
                        "-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"
                      } />
                    </svg>
                  )}
                </button>
              </div>

              {/* Dropdown Results */}
              {searchResults.length > 0 && (
                <div
                  className={
                    "absolute top-full left-0 right-0 mt-2 bg-white border " +
                    "border-[#CED0D4] shadow-xl rounded-lg z-50 max-h-60 " +
                    "overflow-y-auto custom-scrollbar"
                  }
                >
                  {searchResults.map((place, idx) => {
                    const parts = place.display_name.split(",");
                    const mainName = parts[0];
                    const subName = parts.slice(1).join(",").trim();
                    return (
                      <div
                        key={idx}
                        onClick={() => {
                          setLocation(mainName);
                          setSearchResults([]);
                        }}
                        className={
                          "flex items-center gap-3 p-3 hover:bg-[#F0F2F5] " +
                          "cursor-pointer transition-colors border-b " +
                          "border-[#E4E6EB] last:border-0"
                        }
                      >
                        <div
                          className={
                            "w-10 h-10 rounded-full bg-[#E4E6EB] flex " +
                            "items-center justify-center flex-shrink-0"
                          }
                        >
                          <svg
                            className="w-6 h-6 text-[#1C1E21]"
                            fill="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path d={
                              "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 " +
                              "7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-" +
                              "2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 " +
                              "2.5-2.5 2.5z"
                            } />
                          </svg>
                        </div>
                        <div className="flex flex-col overflow-hidden">
                          <span
                            className={
                              "text-[15px] font-semibold text-[#1C1E21] truncate"
                            }
                          >
                            {mainName}
                          </span>
                          <span className="text-[13px] text-[#65676B] truncate">
                            {subName}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* Toolbar */}
          <div
            className={
              "mx-4 mb-3 border border-[#CED0D4] rounded-xl " +
              "px-3 py-2 flex items-center justify-between"
            }
          >
            <span className="text-sm font-semibold text-[#1C1E21]">
              Thêm vào bài viết của bạn
            </span>

            {/* Relative wrapper to anchor the emoji picker precisely above the button */}
            <div className="flex items-center gap-1 relative">

              {/* Full Emoji Picker anchored to the toolbar */}
              {showEmoji && (
                <div
                  className={
                    "absolute right-0 bottom-12 z-[100] shadow-2xl rounded-xl " +
                    "overflow-hidden border border-[#CED0D4]"
                  }
                >
                  <EmojiPicker
                    onEmojiClick={(emojiData) => insertEmoji(emojiData.emoji)}
                    emojiStyle={EmojiStyle.FACEBOOK}
                    theme={Theme.LIGHT}
                    searchPlaceholder="Tìm kiếm cảm xúc..."
                    previewConfig={{ showPreview: false }}
                    skinTonesDisabled={true}
                    width={320}
                    height={320}
                  />
                </div>
              )}

              <button
                onClick={() => {
                  setShowImageInput(p => !p);
                  setActiveTab(p => p === 'image' ? null : 'image');
                  setBgGradient(null);
                  setShowLocationInput(false);
                  setShowTagFriends(false);
                  setShowEmoji(false);
                }}
                className={
                  "w-9 h-9 rounded-full hover:bg-[#F0F2F5] " +
                  "flex items-center justify-center transition-colors " +
                  (activeTab === 'image' ? "bg-[#E7F3FF]" : "bg-transparent")
                }
                title="Photo/Video"
              >
                <svg
                  className="w-6 h-6 text-[#65676B]"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d={
                    "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14" +
                    "c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5" +
                    "l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
                  } />
                </svg>
              </button>

              <button
                onClick={() => {
                  setShowEmoji(p => !p);
                  setActiveTab(p => p === 'emoji' ? null : 'emoji');
                  setShowLocationInput(false);
                  setShowTagFriends(false);
                }}
                className={
                  "w-9 h-9 rounded-full hover:bg-[#F0F2F5] " +
                  "flex items-center justify-center transition-colors " +
                  (activeTab === 'emoji' ? "bg-[#E7F3FF]" : "bg-transparent")
                }
                title="Feeling/Activity"
              >
                <svg
                  className="w-6 h-6 text-[#65676B]"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d={
                    "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 " +
                    "10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8" +
                    "s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 " +
                    "0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5" +
                    "s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5" +
                    "S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 " +
                    "6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 " +
                    "3.5 5.11 3.5z"
                  } />
                </svg>
              </button>

              <button
                onClick={() => {
                  setShowTagFriends(p => !p);
                  setActiveTab(p => p === 'tag' ? null : 'tag');
                  setShowLocationInput(false);
                  setShowEmoji(false);
                }}
                className={
                  "w-9 h-9 rounded-full hover:bg-[#F0F2F5] " +
                  "flex items-center justify-center transition-colors " +
                  (activeTab === 'tag' ? "bg-[#E7F3FF]" : "bg-transparent")
                }
                title="Tag friends"
              >
                <svg
                  className="w-6 h-6 text-[#65676B]"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d={
                    "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 " +
                    "1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2" +
                    "c0-2.66-5.33-4-8-4z"
                  } />
                </svg>
              </button>

              <button
                onClick={() => {
                  setShowLocationInput(p => !p);
                  setActiveTab(p => p === 'location' ? null : 'location');
                  setShowTagFriends(false);
                  setShowEmoji(false);
                }}
                className={
                  "w-9 h-9 rounded-full hover:bg-[#F0F2F5] " +
                  "flex items-center justify-center transition-colors " +
                  (activeTab === 'location' ? "bg-[#E7F3FF]" : "bg-transparent")
                }
                title="Check in"
              >
                <svg
                  className="w-6 h-6 text-[#65676B]"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d={
                    "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 " +
                    "7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-" +
                    "2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 " +
                    "2.5-2.5 2.5z"
                  } />
                </svg>
              </button>
            </div>
          </div>

          {/* Submit */}
          <div className="px-4 pb-4">
            <button
              onClick={handleSubmit}
              disabled={!canSubmit}
              className={`w-full h-10 rounded-lg font-bold text-sm transition-colors ${canSubmit
                  ? "bg-[#1877F2] hover:bg-[#166FE5] text-white"
                  : "bg-[#E4E6EB] text-[#BCC0C4] cursor-not-allowed"
                }`}
            >
              {isEdit ? "Lưu" : "Đăng"}
            </button>
          </div>
        </div>
      </div>
    );
  }
