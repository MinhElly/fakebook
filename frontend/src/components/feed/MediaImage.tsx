import { useEffect, useState } from "react"

import api from "@/services/apis"

import { useAuth } from "@/providers/AuthProvider"

type Props = Omit<React.ImgHTMLAttributes<HTMLImageElement>, "src"> & {
  src: string
  alt: string
}

export default function MediaImage({
  src,
  alt,
  className,
  onClick,
  ...props
}: Props) {
  const { status } = useAuth()

  const [url, setUrl] = useState<string | null>(null)

  const [error, setError] = useState(false)

  useEffect(() => {
    setError(false)
    setUrl(null)

    if (
      status !== "authenticated" ||
      !src.includes("/services/mediaservice/")
    ) {
      setUrl(src)
      return
    }

    const controller = new AbortController()

    let objectUrl: string | undefined

    api
      .get(src, { responseType: "blob", signal: controller.signal })
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data)

        if (controller.signal.aborted) URL.revokeObjectURL(objectUrl)
        else setUrl(objectUrl)
      })
      .catch(() => {
        if (!controller.signal.aborted) setError(true)
      })

    return () => {
      controller.abort()
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [src, status])

  if (error)
    return (
      <span role="status" className="block p-4 text-sm text-[#65676B]">
        Không tải được ảnh.
      </span>
    )

  if (!url)
    return (
      <span
        role="status"
        className="block h-40 animate-pulse bg-gray-100"
        aria-label="Đang tải ảnh"
      />
    )

  return (
    <img
      {...props}
      src={url}
      alt={alt}
      className={className}
      onClick={onClick}
      onError={() => setError(true)}
    />
  )
}
